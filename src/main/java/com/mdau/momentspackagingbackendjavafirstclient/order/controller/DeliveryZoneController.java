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

    @GetMapping("/api/v1/public/delivery-fee")
    public ResponseEntity<Map<String, BigDecimal>> getFee(
            @RequestParam String county) {
        return ResponseEntity.ok(
                Map.of("fee", deliveryZoneService.getFeeForCounty(county)));
    }

    @IsAdmin
    @GetMapping("/api/v1/admin/delivery-zones")
    public ResponseEntity<List<DeliveryZone>> getAll() {
        return ResponseEntity.ok(deliveryZoneService.getAllActive());
    }

    @IsAdmin
    @PostMapping("/api/v1/admin/delivery-zones")
    public ResponseEntity<DeliveryZone> create(
            @RequestBody Map<String, Object> body) {
        String county = (String) body.get("county");
        BigDecimal fee = new BigDecimal(body.get("feeAmount").toString());
        return ResponseEntity.status(201)
                .body(deliveryZoneService.create(county, fee));
    }

    @IsAdmin
    @PatchMapping("/api/v1/admin/delivery-zones/{id}")
    public ResponseEntity<DeliveryZone> update(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String county  = (String) body.get("county");
        BigDecimal fee = body.get("feeAmount") != null ?
                new BigDecimal(body.get("feeAmount").toString()) : null;
        Boolean active = (Boolean) body.get("active");
        return ResponseEntity.ok(deliveryZoneService.update(id, county, fee, active));
    }

    @IsAdmin
    @DeleteMapping("/api/v1/admin/delivery-zones/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deliveryZoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}