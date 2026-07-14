package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FreeDeliveryThreshold;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.FreeDeliveryThresholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FreeDeliveryThresholdController {

    private final FreeDeliveryThresholdService service;

    // ── Public — cart/checkout FAB banner reads this ────────────────────────────

    @GetMapping("/api/v1/public/free-delivery-thresholds")
    public ResponseEntity<List<FreeDeliveryThreshold>> getActive() {
        return ResponseEntity.ok(service.getAllActive());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @IsAdmin
    @GetMapping("/api/v1/admin/free-delivery-thresholds")
    public ResponseEntity<List<FreeDeliveryThreshold>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @IsAdmin
    @PostMapping("/api/v1/admin/free-delivery-thresholds")
    public ResponseEntity<FreeDeliveryThreshold> create(@RequestBody Map<String, Object> body) {
        BigDecimal minOrderAmount = new BigDecimal(body.get("minOrderAmount").toString());
        String zoneLabel = (String) body.get("zoneLabel");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        return ResponseEntity.status(201).body(service.create(minOrderAmount, zoneLabel, sortOrder));
    }

    @IsAdmin
    @PatchMapping("/api/v1/admin/free-delivery-thresholds/{id}")
    public ResponseEntity<FreeDeliveryThreshold> update(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        BigDecimal minOrderAmount = body.get("minOrderAmount") != null
                ? new BigDecimal(body.get("minOrderAmount").toString()) : null;
        String zoneLabel = (String) body.get("zoneLabel");
        Boolean active = (Boolean) body.get("active");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        return ResponseEntity.ok(service.update(id, minOrderAmount, zoneLabel, active, sortOrder));
    }

    @IsAdmin
    @DeleteMapping("/api/v1/admin/free-delivery-thresholds/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
