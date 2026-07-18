package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService    orderService;
    private final AuditLogService auditLogService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<OrderDto>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(status, customerId, pageable));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @IsStaffOrAdmin
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable UUID id,
            @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        OrderDto before = orderService.getById(id);
        OrderDto updated = orderService.updateStatus(
                id, request.getStatus(), request.getStaffNotes(), user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "STATUS_CHANGE", request.getStaffNotes(),
                "{\"from\":\"" + before.getStatus() + "\",\"to\":\"" + updated.getStatus() + "\"}",
                AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    @IsStaffOrAdmin
    @PatchMapping("/{id}/assign")
    public ResponseEntity<OrderDto> assign(
            @PathVariable UUID id,
            @RequestBody OrderAssignRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        OrderDto updated = orderService.assignOrder(id, request.getAssignedTo());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "ASSIGN", null,
                "{\"assignedTo\":\"" + request.getAssignedTo() + "\"}",
                AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    @IsStaffOrAdmin
    @PatchMapping("/{id}/dispatch-confirm")
    public ResponseEntity<OrderDto> dispatchConfirm(
            @PathVariable UUID id,
            @RequestBody DispatchConfirmRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        OrderDto updated = orderService.dispatchConfirm(
                id,
                request.getDeliveryConfirmationStatus(),
                request.getContentsVerified(),
                user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "DISPATCH_CONFIRM", null,
                "{\"contentsVerified\":" + request.getContentsVerified() + "}",
                AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    /**
     * Logs a refund complaint for an admin to review — deliberately makes no financial or
     * inventory changes by itself. Any staff member can log one; only an Admin can act on it
     * via the endpoints below.
     */
    @IsStaffOrAdmin
    @PatchMapping("/{id}/refund-request")
    public ResponseEntity<OrderDto> requestRefund(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        String reason = body.getOrDefault("reason", "Refund requested");
        OrderDto updated = orderService.requestRefund(id, reason, user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "REFUND_REQUESTED", reason, null, AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    @IsStaffOrAdmin
    @PatchMapping("/{id}/refund-request/resolve")
    public ResponseEntity<OrderDto> resolveRefundRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        OrderDto updated = orderService.resolveRefundRequest(id, user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "REFUND_REQUEST_RESOLVED", null, null, AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    /** Explicit, admin-only action — marks the order's payment as refunded/failed. Never automatic. */
    @IsAdmin
    @PatchMapping("/{id}/mark-payment-refunded")
    public ResponseEntity<OrderDto> markPaymentFailed(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        String reason = body.getOrDefault("reason", "Refund processed");
        OrderDto updated = orderService.markPaymentFailed(id, reason, user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "PAYMENT_MARKED_REFUNDED", reason, null, AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    /** Explicit, admin-only action — restores stock for this order. Never automatic. */
    @IsAdmin
    @PatchMapping("/{id}/restore-inventory")
    public ResponseEntity<OrderDto> restoreInventory(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        OrderDto updated = orderService.restoreInventory(id, user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "INVENTORY_RESTORED", null, null, AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }
}