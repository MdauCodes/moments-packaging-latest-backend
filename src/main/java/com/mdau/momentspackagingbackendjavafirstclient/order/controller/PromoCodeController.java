package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PromoCode;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.PromoCodeService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping("/api/v1/checkout/validate-promo")
    public ResponseEntity<Map<String, Object>> validatePromo(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal(errorOnInvalidType = false) User user) {
        String code      = (String) body.get("code");
        BigDecimal sub   = new BigDecimal(body.get("subtotal").toString());
        UUID userId      = user != null ? user.getId() : null;
        return ResponseEntity.ok(promoCodeService.validateAndCalculate(code, sub, userId));
    }

    @IsAdmin
    @GetMapping("/api/v1/admin/promo-codes")
    public ResponseEntity<List<PromoCode>> getAll() {
        return ResponseEntity.ok(promoCodeService.getAll());
    }

    @IsAdmin
    @PostMapping("/api/v1/admin/promo-codes")
    public ResponseEntity<PromoCode> create(@RequestBody PromoCode promo) {
        return ResponseEntity.status(201).body(promoCodeService.create(promo));
    }

    @IsAdmin
    @PatchMapping("/api/v1/admin/promo-codes/{id}")
    public ResponseEntity<PromoCode> update(
            @PathVariable UUID id, @RequestBody PromoCode updates) {
        return ResponseEntity.ok(promoCodeService.update(id, updates));
    }

    @IsAdmin
    @DeleteMapping("/api/v1/admin/promo-codes/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        promoCodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}