package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RisellerProperties;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerCatalogItem;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerStockItem;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.RisellerStockPage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // -- Catalog sync (every 2 days) --

    @Transactional
    public void syncCatalog() {
        log.info("RisellerSyncService: starting catalog sync from S3");
        try {
            List<RisellerCatalogItem> items = downloadAndParseCatalog();
            int matched = 0;
            int skipped = 0;

            for (RisellerCatalogItem item : items) {
                if (item.getCode() == null || item.getId() == null) { skipped++; continue; }
                String code = item.getCode().trim();
                String uuid = item.getId().trim();

                // Find product by keyword matching the Riseller code
                productRepository.findByKeywordAndDeletedFalse(code).ifPresentOrElse(product -> {
                    if (!uuid.equals(product.getRisellerItemId())) {
                        product.setRisellerItemId(uuid);
                        productRepository.save(product);
                    }
                }, () -> log.debug("No product found for Riseller code {}", code));

                matched++;
            }

            settingsService.upsertSetting(buildSetting(KEY_LAST_CATALOG_SYNC, Instant.now().toString()));
            auditLogService.logSystem("SYSTEM", null, "riseller-catalog-sync", "CATALOG_SYNC", null, "{\"matched\":" + matched + ",\"skipped\":" + skipped + "}");
            log.info("Catalog sync complete: {} items processed, {} skipped", matched, skipped);

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