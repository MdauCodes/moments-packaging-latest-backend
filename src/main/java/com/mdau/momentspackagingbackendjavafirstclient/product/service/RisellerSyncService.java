package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RisellerProperties;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerCatalogItem;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerStockItem;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RisellerSyncService {

    private final ProductRepository   productRepository;
    private final RisellerProperties  props;
    private final ObjectMapper        objectMapper;
    private final SettingsService     settingsService;
    private final EmailService        emailService;
    private final AuditLogService     auditLogService;

    private static final String KEY_LAST_STOCK_SYNC  = "riseller.last_stock_sync";
    private static final String KEY_STOCK_FAIL_COUNT = "riseller.stock_fail_count";
    private static final String KEY_LAST_CATALOG_SYNC = "riseller.last_catalog_sync";
    private static final int    FAIL_ALERT_THRESHOLD  = 3;

    // Minimum Jaccard score for a candidate to be considered at all
    private static final double AUTO_LINK_THRESHOLD = 0.55;
    // Minimum score gap between best and runner-up to auto-link without ambiguity
    private static final double MIN_CONFIDENCE_GAP  = 0.15;

    // -- Catalog sync (every 2 days) --

    @Transactional
    public void syncCatalog() {
        log.info("RisellerSyncService: starting catalog sync from S3");
        try {
            List<RisellerCatalogItem> catalogItems = downloadAndParseCatalog();

            // Build set of all valid Riseller IDs in this catalog snapshot
            Set<String> catalogIds = catalogItems.stream()
                    .filter(i -> i.getId() != null)
                    .map(i -> i.getId().trim())
                    .collect(Collectors.toSet());

            // Load products not yet linked — candidates for fuzzy auto-matching
            // Use a mutable list so we can remove each product once claimed
            List<Product> unlinkedPool = new ArrayList<>(productRepository.findUnlinkedProducts());

            int exactLinked    = 0;  // linked via keyword / tag / code-in-name
            int autoLinked     = 0;  // linked via fuzzy name match (clear winner)
            int autoCreated    = 0;  // new product created from Riseller (no DB match existed)
            int ambiguous      = 0;  // multiple similar matches — not auto-linked
            int alreadySynced  = 0;  // already had correct risellerItemId
            int priceUpdated   = 0;  // price refreshed from Riseller on already-linked product
            int skipped        = 0;  // missing code or id in catalog entry

            StringBuilder autoLinkLog   = new StringBuilder();
            StringBuilder ambiguousLog  = new StringBuilder();

            for (RisellerCatalogItem item : catalogItems) {
                if (item.getCode() == null || item.getId() == null) { skipped++; continue; }
                String code = item.getCode().trim();
                String uuid = item.getId().trim();

                // ── Fast path: product already linked with this exact Riseller ID ──
                var existingOpt = productRepository.findByRisellerItemIdAndDeletedFalse(uuid);
                if (existingOpt.isPresent()) {
                    Product existing = existingOpt.get();
                    boolean changed = false;
                    // Keep price in sync with Riseller — admin never needs to set it manually
                    if (item.getPriceInc() != null && item.getPriceInc() > 0) {
                        BigDecimal risellerPrice = BigDecimal.valueOf(item.getPriceInc())
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        if (!risellerPrice.equals(existing.getBasePrice())) {
                            existing.setBasePrice(risellerPrice);
                            changed = true;
                            priceUpdated++;
                            log.info("Price updated for \"{}\": {} → {} KES",
                                    existing.getName(), existing.getBasePrice(), risellerPrice);
                        }
                    }
                    // Fill in description/category only if admin hasn't set them yet
                    if (existing.getDescription() == null || existing.getDescription().isBlank()) {
                        existing.setDescription(generateDescription(item));
                        changed = true;
                    }
                    if (existing.getCategory() == null || existing.getCategory().isBlank()) {
                        existing.setCategory(inferCategory(item.getName()));
                        changed = true;
                    }
                    if (changed) productRepository.save(existing);
                    alreadySynced++;
                    continue;
                }

                // ── Resolve: exact tiers first, then fuzzy with ambiguity detection ─
                var outcome = RisellerNameMatcher.resolve(
                        code, item.getName(), unlinkedPool,
                        AUTO_LINK_THRESHOLD, MIN_CONFIDENCE_GAP);

                switch (outcome) {
                    case RisellerNameMatcher.MatchOutcome.Clear(var m) -> {
                        var p = m.product();
                        boolean wasReactivated = Boolean.TRUE.equals(p.getRisellerSuspended());
                        p.setRisellerItemId(uuid);
                        p.setRisellerSuspended(false);  // lift suspension if previously suspended
                        // Stamp the code as a keyword → next sync uses the exact fast-path
                        if (p.getKeywords() == null) p.setKeywords(new ArrayList<>());
                        if (!p.getKeywords().contains(code)) p.getKeywords().add(code);
                        productRepository.save(p);
                        unlinkedPool.remove(p);

                        boolean wasExact = m.reason().contains("exact") || m.reason().contains("code-in-name");
                        if (wasReactivated) {
                            exactLinked++;
                            log.info("Catalog RE-ACTIVATED [{}]: \"{}\" ← code={} (was suspended)",
                                    m.reason(), p.getName(), code);
                        } else if (wasExact) {
                            exactLinked++;
                            log.info("Catalog linked [{}]: \"{}\" ← code={}", m.reason(), p.getName(), code);
                        } else {
                            autoLinked++;
                            autoLinkLog.append(String.format(
                                    "  • Riseller \"%s\" (code=%s) → DB \"%s\" [%s]%n",
                                    item.getName(), code, p.getName(), m.reason()));
                            log.info("Catalog auto-linked [{}]: Riseller \"{}\" (code={}) → DB \"{}\"",
                                    m.reason(), item.getName(), code, p.getName());
                        }
                    }

                    case RisellerNameMatcher.MatchOutcome.Ambiguous(var candidates) -> {
                        ambiguous++;
                        // Log all candidates so the admin email is actionable
                        ambiguousLog.append(String.format(
                                "%n  Riseller \"%s\" (code=%s) — %d similar products, none auto-linked:%n",
                                item.getName(), code, candidates.size()));
                        candidates.forEach(c -> ambiguousLog.append(String.format(
                                "      score=%.2f  DB: \"%s\"%n", c.score(), c.product().getName())));
                        ambiguousLog.append(String.format(
                                "    → Fix: add keyword \"%s\" to the correct product.%n", code));
                        log.warn("Catalog AMBIGUOUS (code={}): \"{}\" — {} candidates too close to auto-link: {}",
                                code, item.getName(), candidates.size(),
                                candidates.stream()
                                        .map(c -> String.format("\"%.20s\"(%.2f)", c.product().getName(), c.score()))
                                        .collect(Collectors.joining(", ")));
                    }

                    case RisellerNameMatcher.MatchOutcome.NotFound() -> {
                        // No existing product matches — Riseller is source of truth, so create it
                        Product created = buildProductFromCatalogItem(item, uuid, code);
                        productRepository.save(created);
                        autoCreated++;
                        log.info("Catalog auto-created: \"{}\" (code={})", created.getName(), code);
                    }
                }
            }

            // ── Orphan cleanup ───────────────────────────────────────────────────────
            // Products whose risellerItemId no longer exists in the current catalog.
            // We never delete a product the admin has worked on — we suspend it instead.
            // It will be automatically re-activated if Riseller re-lists the item.
            int orphanSuspended = 0;
            int orphanDeleted   = 0;
            StringBuilder suspendedLog = new StringBuilder();

            if (!catalogIds.isEmpty()) {
                List<Product> orphans = productRepository.findOrphanedByRisellerId(catalogIds);

                for (var orphan : orphans) {
                    // Image is the only thing admin uploads — description/price/category come from Riseller.
                    // Only preserve (suspend) products where admin has actually uploaded an image.
                    boolean hasAdminContent =
                            orphan.getPrimaryImageUrl() != null ||
                            (orphan.getImageUrls() != null && !orphan.getImageUrls().isEmpty());

                    orphan.setRisellerItemId(null);  // unlink in all cases

                    if (hasAdminContent) {
                        // Admin uploaded an image — suspend (hide from store) but keep everything
                        orphan.setRisellerSuspended(true);
                        orphan.setStockStatus(StockStatus.OUT_OF_STOCK);
                        productRepository.save(orphan);
                        orphanSuspended++;
                        suspendedLog.append(String.format("  • \"%s\"%n", orphan.getName()));
                        log.warn("Orphan SUSPENDED (has images): \"{}\" — Riseller no longer lists this item",
                                orphan.getName());
                    } else {
                        // No admin images — safe to remove (price/description were auto-set by sync)
                        orphan.setDeleted(true);
                        productRepository.save(orphan);
                        orphanDeleted++;
                        log.info("Orphan DELETED (no images): \"{}\"", orphan.getName());
                    }
                }
            }

            // ── Persist stats & audit ─────────────────────────────────────────────
            settingsService.upsertSetting(buildSetting(KEY_LAST_CATALOG_SYNC, Instant.now().toString()));
            String auditPayload = String.format(
                    "{\"exactLinked\":%d,\"autoLinked\":%d,\"autoCreated\":%d,\"ambiguous\":%d," +
                    "\"alreadySynced\":%d,\"priceUpdated\":%d,\"skipped\":%d," +
                    "\"orphanSuspended\":%d,\"orphanDeleted\":%d}",
                    exactLinked, autoLinked, autoCreated, ambiguous,
                    alreadySynced, priceUpdated, skipped, orphanSuspended, orphanDeleted);
            auditLogService.logSystem("SYSTEM", null, "riseller-catalog-sync", "CATALOG_SYNC", null, auditPayload);

            log.info("Catalog sync complete: {} exact-linked, {} fuzzy-linked, {} auto-created, " +
                     "{} ambiguous (needs review), {} already synced ({} prices refreshed), {} skipped | " +
                     "Orphans: {} suspended (images preserved), {} deleted (no images)",
                    exactLinked, autoLinked, autoCreated, ambiguous,
                    alreadySynced, priceUpdated, skipped, orphanSuspended, orphanDeleted);

            boolean needsEmail = autoLinked > 0 || autoCreated > 0 || ambiguous > 0 || orphanSuspended > 0;
            if (needsEmail) {
                StringBuilder email = new StringBuilder("Riseller catalog sync completed.\n\n");

                if (autoCreated > 0) {
                    email.append("── AUTO-CREATED ").append(autoCreated).append(" new product(s) ──\n");
                    email.append("These Riseller items had no matching product in the DB and were created automatically.\n");
                    email.append("Price, category, and a placeholder description have been pre-filled from Riseller data.\n");
                    email.append("The only thing needed from you: upload product images in the admin panel.\n");
                    email.append("Products remain OUT_OF_STOCK until the next stock sync confirms inventory.\n\n");
                }

                if (autoLinked > 0) {
                    email.append("── AUTO-LINKED ").append(autoLinked).append(" product(s) by name similarity ──\n");
                    email.append("Review and correct if any link looks wrong:\n");
                    email.append(autoLinkLog);
                    email.append("Fix: add keyword \"<RisellerCode>\" to the correct product; next sync corrects it.\n\n");
                }

                if (ambiguous > 0) {
                    email.append("── AMBIGUOUS ").append(ambiguous).append(" item(s) — manual action required ──\n");
                    email.append("Multiple existing products matched too closely to auto-link safely.\n");
                    email.append("For each item, add the Riseller code as a keyword to the correct product:\n");
                    email.append(ambiguousLog).append("\n");
                }

                if (orphanSuspended > 0) {
                    email.append("── SUSPENDED ").append(orphanSuspended).append(" product(s) (Riseller no longer lists them) ──\n");
                    email.append("These products have been hidden from the storefront but all admin content\n");
                    email.append("(images, descriptions, prices) is fully preserved. They will reappear automatically\n");
                    email.append("if Riseller re-lists them. You may also manually re-enable or delete them.\n");
                    email.append(suspendedLog);
                }

                emailService.sendSyncFailureAlert(email.toString());
            }

        } catch (Exception e) {
            log.error("Catalog sync failed: {}", e.getMessage(), e);
            emailService.sendSyncFailureAlert("Catalog sync failed: " + e.getMessage());
        }
    }

    // -- Stock sync (every hour) --

    @Transactional
    public void syncStock() {
        log.info("RisellerSyncService: starting stock sync");
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X_APP_KEY", props.getXAppKey());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            int page = 1;
            boolean hasNext = true;
            int totalUpdated = 0;
            int totalUnmatched = 0;

            while (hasNext) {
                String url = props.getStockUrl() + "?pageSize=" + props.getPageSize() + "&page=" + page;
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    throw new RuntimeException("Stock API returned " + response.getStatusCode());
                }

                Map<?, ?> result = (Map<?, ?>) response.getBody().get("result");
                hasNext = Boolean.TRUE.equals(result.get("hasNextPage"));

                List<?> data = (List<?>) result.get("data");
                List<RisellerStockItem> items = objectMapper.convertValue(
                        data, new TypeReference<List<RisellerStockItem>>() {});

                for (RisellerStockItem item : items) {
                    if (item.getItemId() == null) continue;
                    var productOpt = productRepository.findByRisellerItemIdAndDeletedFalse(item.getItemId());
                    if (productOpt.isEmpty()) { totalUnmatched++; continue; }

                    var product = productOpt.get();

                    // MADE_TO_ORDER products don't track physical stock — skip entirely.
                    if (product.getStockStatus() == StockStatus.MADE_TO_ORDER) continue;

                    int newCount = item.getInStock() != null ? item.getInStock().intValue() : 0;

                    StockStatus newStatus;
                    if (newCount <= 0) {
                        newStatus = StockStatus.OUT_OF_STOCK;
                    } else if (newCount <= product.getLowStockThreshold()) {
                        newStatus = StockStatus.LOW_STOCK;
                    } else {
                        newStatus = StockStatus.IN_STOCK;
                    }

                    if (product.getStockCount() != newCount || product.getStockStatus() != newStatus) {
                        log.info("Stock updated: \"{}\" count={}→{} status={}{}",
                                product.getName(), product.getStockCount(), newCount,
                                newStatus,
                                product.getStockStatus() != newStatus
                                        ? " (was " + product.getStockStatus() + ")" : "");
                        productRepository.setStockCount(product.getId(), newCount);
                        totalUpdated++;
                    }
                }

                log.debug("Stock sync page {}: {} items processed", page, items.size());
                page++;
            }

            // Reset failure counter on success
            settingsService.upsertSetting(buildSetting(KEY_STOCK_FAIL_COUNT, "0"));
            settingsService.upsertSetting(buildSetting(KEY_LAST_STOCK_SYNC, Instant.now().toString()));
            auditLogService.logSystem("SYSTEM", null, "riseller-stock-sync", "STOCK_SYNC", null, "{\"updated\":" + totalUpdated + ",\"unmatched\":" + totalUnmatched + "}");
            log.info("Stock sync complete: {} products updated, {} unmatched", totalUpdated, totalUnmatched);

        } catch (Exception e) {
            log.error("Stock sync failed: {}", e.getMessage(), e);
            handleStockSyncFailure(e.getMessage());
        }
    }

    // -- Catalog item helpers --

    private Product buildProductFromCatalogItem(RisellerCatalogItem item, String uuid, String code) {
        String cleanName = RisellerNameMatcher.cleanDisplayName(item.getName());

        BigDecimal price = (item.getPriceInc() != null && item.getPriceInc() > 0)
                ? BigDecimal.valueOf(item.getPriceInc()).setScale(2, java.math.RoundingMode.HALF_UP)
                : null;

        BigDecimal vat = (item.getVatRate() != null)
                ? BigDecimal.valueOf(item.getVatRate()).setScale(4, java.math.RoundingMode.HALF_UP)
                : new BigDecimal("0.1600");

        return Product.builder()
                .name(cleanName)
                .slug(generateUniqueSlug(cleanName))
                .risellerItemId(uuid)
                .keywords(new ArrayList<>(List.of(code)))
                .stockStatus(StockStatus.OUT_OF_STOCK)
                .stockCount(0)
                .basePrice(price)
                .vatRate(vat)
                .vatExempt(false)
                .category(inferCategory(item.getName()))
                .description(generateDescription(item))
                .build();
    }

    // -- Catalog enrichment helpers --

    private static final Pattern PACK_PARENS = Pattern.compile("\\(([^)]+)\\)");

    static String generateDescription(RisellerCatalogItem item) {
        String cleanName = RisellerNameMatcher.cleanDisplayName(item.getName());
        StringBuilder sb = new StringBuilder(toTitleCase(cleanName)).append(".");

        // Pack size from name parens: "(100PCS)", "(1*10KGS)", "(50PKTS*50PCS)"
        if (item.getName() != null) {
            Matcher m = PACK_PARENS.matcher(item.getName());
            if (m.find()) {
                sb.append(" Pack size: ").append(m.group(1)).append(".");
            }
        }

        // Brand (prefer BrandName, fall back to ManufacturerName)
        String brand = (item.getBrandName() != null && !item.getBrandName().isBlank())
                ? item.getBrandName()
                : item.getManufacturerName();
        if (brand != null && !brand.isBlank()) {
            sb.append(" Supplied by ").append(toTitleCase(brand)).append(".");
        }

        if (item.getBaseUomName() != null && !item.getBaseUomName().isBlank()) {
            sb.append(" Sold per ").append(item.getBaseUomName().toLowerCase(Locale.ROOT)).append(".");
        }

        sb.append(" Contact us for bulk pricing or custom branding options.");
        return sb.toString();
    }

    static String inferCategory(String name) {
        if (name == null) return "GENERAL";
        String u = name.toUpperCase(Locale.ROOT);

        if (u.contains("CUP") || u.contains("RIPPLE") || u.contains("JAZI")
                || u.contains("THERMO") || u.contains("LID") || u.contains("ICE CREAM"))
            return "CUPS & LIDS";
        if (u.contains("PLATE"))
            return "PLATES";
        if (u.contains("BAG") || u.contains("KHAKI") || u.contains("ZIPLOCK")
                || u.contains("POUCH") || u.contains("MAFUCO") || u.contains("NICE #")
                || u.contains("BLUESTAR") || u.contains("BARAKA"))
            return "BAGS";
        if (u.contains("BOARD"))
            return "BOARDS";
        if (u.contains("FOIL"))
            return "FOIL TINS";
        if (u.contains("BOX") || u.contains("MUFFIN") || u.contains("CUPCAKE")
                || u.contains("CASE") || u.contains("CAKE CONT"))
            return "BOXES & BAKING";
        if (u.contains("CONT") || u.contains("SUSHI") || u.contains("TIRAMISU")
                || u.contains("OCTA") || u.contains("PULP") || u.contains("HOT DOG")
                || u.contains("TRAY") || u.contains("PET ") || u.contains("MACAROON")
                || u.contains("LOAF") || u.contains("KRAFT"))
            return "CONTAINERS";
        if (u.contains("CLING") || u.contains("FILM") || u.contains("POLYCOATED"))
            return "FILM & WRAP";
        if (u.contains("JAR"))
            return "JARS";
        if (u.contains("TWINE") || u.contains("STRAW"))
            return "ACCESSORIES";
        if (u.contains("SHOES") || u.contains("GLOVES") || u.contains("APRON")
                || u.contains("MASK") || u.contains("CHEF"))
            return "HYGIENE & PPE";

        return "GENERAL";
    }

    private static String toTitleCase(String s) {
        if (s == null || s.isBlank()) return s;
        String[] words = s.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .replaceAll("\\s+", "-");
        if (base.isBlank()) base = "product";
        String slug = base;
        int n = 1;
        while (productRepository.existsBySlug(slug)) slug = base + "-" + n++;
        return slug;
    }

    // -- S3 download --

    private List<RisellerCatalogItem> downloadAndParseCatalog() throws Exception {
        RisellerProperties.S3 s3cfg = props.getS3();

        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(s3cfg.getServiceUrl()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3cfg.getAccessKey(), s3cfg.getSecretKey())))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();

        String objectKey = s3cfg.getFolderName() + "/" + s3cfg.getFileName();
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(s3cfg.getBucketName())
                .key(objectKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3stream = s3.getObject(req);
             ZipInputStream zip = new ZipInputStream(s3stream)) {

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".json")) {
                    byte[] bytes = readAllBytes(zip);
                    return objectMapper.readValue(bytes,
                            new TypeReference<List<RisellerCatalogItem>>() {});
                }
            }
        }
        throw new RuntimeException("No JSON file found in item_cache.zip");
    }

    private byte[] readAllBytes(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }

    // -- Failure tracking --

    private void handleStockSyncFailure(String message) {
        try {
            int fails = Integer.parseInt(
                    settingsService.getValue(KEY_STOCK_FAIL_COUNT, "0")) + 1;
            settingsService.upsertSetting(buildSetting(KEY_STOCK_FAIL_COUNT, String.valueOf(fails)));

            if (fails >= FAIL_ALERT_THRESHOLD) {
                log.warn("Stock sync has failed {} consecutive times -- sending alert", fails);
                emailService.sendSyncFailureAlert(
                        "Riseller stock sync has failed " + fails +
                        " consecutive times. Last error: " + message);
                settingsService.upsertSetting(buildSetting(KEY_STOCK_FAIL_COUNT, "0"));
            }
        } catch (Exception ex) {
            log.error("Failed to track sync failure: {}", ex.getMessage());
        }
    }

    private com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest
            buildSetting(String key, String value) {
        var req = new com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest();
        req.setKey(key);
        req.setValue(value);
        return req;
    }
}