package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RisellerProperties;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerCatalogItem;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerStockItem;
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
import java.net.URI;
import java.time.Instant;
import java.util.*;
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
            List<com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product>
                    unlinkedPool = new ArrayList<>(productRepository.findUnlinkedProducts());

            int exactLinked   = 0;  // linked via keyword / tag / code-in-name
            int autoLinked    = 0;  // linked via fuzzy name match (clear winner)
            int ambiguous     = 0;  // multiple similar matches — not auto-linked
            int alreadySynced = 0;  // already had correct risellerItemId
            int skipped       = 0;  // missing code or id in catalog entry
            int noMatch       = 0;  // no product found even after fuzzy search

            StringBuilder autoLinkLog   = new StringBuilder();
            StringBuilder ambiguousLog  = new StringBuilder();

            for (RisellerCatalogItem item : catalogItems) {
                if (item.getCode() == null || item.getId() == null) { skipped++; continue; }
                String code = item.getCode().trim();
                String uuid = item.getId().trim();

                // ── Fast path: product already linked with this exact Riseller ID ──
                if (productRepository.findByRisellerItemIdAndDeletedFalse(uuid).isPresent()) {
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
                        p.setRisellerItemId(uuid);
                        // Stamp the code as a keyword → next sync uses the exact fast-path
                        if (p.getKeywords() == null) p.setKeywords(new ArrayList<>());
                        if (!p.getKeywords().contains(code)) p.getKeywords().add(code);
                        productRepository.save(p);
                        unlinkedPool.remove(p);

                        boolean wasExact = m.reason().contains("exact") || m.reason().contains("code-in-name");
                        if (wasExact) {
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
                        noMatch++;
                        log.debug("No DB product found for Riseller: code={} name=\"{}\"",
                                code, item.getName());
                    }
                }
            }

            // ── Orphan cleanup ───────────────────────────────────────────────────────
            // Products whose risellerItemId no longer exists in the catalog
            // (Riseller removed the item entirely)
            int orphanUnlinked = 0;
            int orphanDeleted  = 0;
            if (!catalogIds.isEmpty()) {
                List<com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product>
                        orphans = productRepository.findOrphanedByRisellerId(catalogIds);

                for (var orphan : orphans) {
                    boolean hasImage = orphan.getPrimaryImageUrl() != null
                            || (orphan.getImageUrls() != null && !orphan.getImageUrls().isEmpty());

                    if (hasImage) {
                        // Safe to unlink — don't delete a product an admin invested in
                        orphan.setRisellerItemId(null);
                        productRepository.save(orphan);
                        orphanUnlinked++;
                        log.warn("Orphan UNLINKED (has image, kept): \"{}\" — Riseller no longer has this item",
                                orphan.getName());
                    } else {
                        // No image, no Riseller match → soft-delete
                        orphan.setRisellerItemId(null);
                        orphan.setDeleted(true);
                        productRepository.save(orphan);
                        orphanDeleted++;
                        log.info("Orphan SOFT-DELETED (no image): \"{}\"", orphan.getName());
                    }
                }
            }

            // ── Persist stats & audit ─────────────────────────────────────────────
            settingsService.upsertSetting(buildSetting(KEY_LAST_CATALOG_SYNC, Instant.now().toString()));
            String auditPayload = String.format(
                    "{\"exactLinked\":%d,\"autoLinked\":%d,\"ambiguous\":%d,\"alreadySynced\":%d," +
                    "\"noMatch\":%d,\"skipped\":%d,\"orphanUnlinked\":%d,\"orphanDeleted\":%d}",
                    exactLinked, autoLinked, ambiguous, alreadySynced, noMatch, skipped,
                    orphanUnlinked, orphanDeleted);
            auditLogService.logSystem("SYSTEM", null, "riseller-catalog-sync", "CATALOG_SYNC", null, auditPayload);

            log.info("Catalog sync complete: {} exact-linked, {} auto-linked by name, {} ambiguous (needs review), " +
                     "{} already synced, {} unmatched, {} skipped | " +
                     "Orphans: {} unlinked (kept), {} soft-deleted",
                    exactLinked, autoLinked, ambiguous, alreadySynced, noMatch, skipped,
                    orphanUnlinked, orphanDeleted);

            // Email admin for auto-links (they should review) and ambiguous cases (they MUST resolve)
            boolean needsEmail = autoLinked > 0 || ambiguous > 0;
            if (needsEmail) {
                StringBuilder email = new StringBuilder("Riseller catalog sync completed. Action may be required.\n\n");

                if (autoLinked > 0) {
                    email.append("── AUTO-LINKED ").append(autoLinked).append(" product(s) by name similarity ──\n");
                    email.append("These were linked automatically. Review and correct if wrong:\n");
                    email.append(autoLinkLog);
                    email.append("To fix a wrong link: add keyword \"<RisellerCode>\" to the correct product,\n");
                    email.append("remove it from the wrong one, then the next sync will correct it.\n\n");
                }

                if (ambiguous > 0) {
                    email.append("── AMBIGUOUS ").append(ambiguous).append(" item(s) — NOT auto-linked ──\n");
                    email.append("Multiple products scored too similarly to pick automatically.\n");
                    email.append("For each item below, add the Riseller code as a keyword to the correct product:\n");
                    email.append(ambiguousLog);
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