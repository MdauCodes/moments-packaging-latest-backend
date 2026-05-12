package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.DeliveryZone;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.DeliveryZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DeliveryZoneController {

    private final DeliveryZoneService deliveryZoneService;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/public/delivery-zones")
    public ResponseEntity<List<DeliveryZone>> getActiveZones() {
        return ResponseEntity.ok(deliveryZoneService.getAllActive());
    }

    @GetMapping("/api/v1/public/delivery-fee")
    public ResponseEntity<Map<String, BigDecimal>> getFee(@RequestParam String county) {
        return ResponseEntity.ok(Map.of("fee", deliveryZoneService.getFeeForCounty(county)));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Admin sees ALL zones including inactive — to allow re-activation */
    @IsAdmin
    @GetMapping("/api/v1/admin/delivery-zones")
    public ResponseEntity<List<DeliveryZone>> getAll() {
        return ResponseEntity.ok(deliveryZoneService.getAll());
    }

    @IsAdmin
    @PostMapping("/api/v1/admin/delivery-zones")
    public ResponseEntity<DeliveryZone> create(@RequestBody Map<String, Object> body) {
        String zoneName   = (String) body.get("zoneName");
        String county     = (String) body.get("county");
        BigDecimal fee    = new BigDecimal(body.get("feeAmount").toString());
        String description= (String) body.getOrDefault("description", null);
        return ResponseEntity.status(201)
                .body(deliveryZoneService.create(zoneName, county, fee, description));
    }

    @IsAdmin
    @PatchMapping("/api/v1/admin/delivery-zones/{id}")
    public ResponseEntity<DeliveryZone> update(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String zoneName    = (String) body.get("zoneName");
        String county      = (String) body.get("county");
        BigDecimal fee     = body.get("feeAmount") != null
                ? new BigDecimal(body.get("feeAmount").toString()) : null;
        Boolean active     = (Boolean) body.get("active");
        String description = (String) body.get("description");
        return ResponseEntity.ok(
                deliveryZoneService.update(id, zoneName, county, fee, active, description));
    }

    @IsAdmin
    @DeleteMapping("/api/v1/admin/delivery-zones/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deliveryZoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}