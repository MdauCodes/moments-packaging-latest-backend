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

    @IsAdmin
    @PatchMapping("/{id}/refund")
    public ResponseEntity<OrderDto> refund(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        String reason = body.getOrDefault("reason", "Refund processed");
        OrderDto updated = orderService.processRefund(id, reason, user.getEmail());
        auditLogService.log(user, "ORDER", id.toString(), updated.getReference(),
                "REFUND", reason, null, AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }
}