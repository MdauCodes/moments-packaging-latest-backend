package com.mdau.momentspackagingbackendjavafirstclient.devtools.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunRequest;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunResult;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.service.DevToolsService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Super-admin-only testing toolkit — everything here is designed to never touch real customer
 * data (no Order, PaymentRecord or Cart rows are created), so it's safe to hit repeatedly while
 * developing/debugging. See DevToolsService for the specifics of how each guarantee is kept.
 */
@RestController
@RequestMapping("/api/v1/admin/dev-tools")
@RequiredArgsConstructor
@IsAdmin
public class AdminDevToolsController {

    private final DevToolsService devToolsService;

    @PostMapping("/checkout-dry-run")
    public CheckoutDryRunResult dryRunCheckout(@RequestBody CheckoutDryRunRequest request) {
        return devToolsService.dryRunCheckout(request);
    }

    @PostMapping("/stk-push-test")
    public Map<String, String> testStkPush(@RequestBody StkPushTestRequest request) {
        String checkoutRequestId = devToolsService.testStkPush(request.getPhone(), request.getAmount());
        return Map.of("checkoutRequestId", checkoutRequestId, "status", "SENT");
    }

    @GetMapping("/tax-invoice-preview/{orderReference}")
    public ResponseEntity<byte[]> previewTaxInvoice(@PathVariable String orderReference) {
        byte[] pdf = devToolsService.previewTaxInvoice(orderReference);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview-" + orderReference + ".pdf\"")
                .body(pdf);
    }

    @Getter
    @Setter
    public static class StkPushTestRequest {
        @NotBlank
        private String phone;
        @NotNull
        @Positive
        private BigDecimal amount;
    }
}
