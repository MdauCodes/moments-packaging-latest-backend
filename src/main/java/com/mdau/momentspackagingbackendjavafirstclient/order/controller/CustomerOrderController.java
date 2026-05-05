package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.cart.dto.CartDto;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<PageResponse<OrderDto>> getMyOrders(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(user, pageable));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<OrderDto> getOrder(
            @AuthenticationPrincipal User user,
            @PathVariable String reference) {
        return ResponseEntity.ok(orderService.getByReference(reference));
    }

    @PostMapping("/{reference}/cancel")
    public ResponseEntity<OrderDto> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable String reference) {
        OrderDto order = orderService.getByReference(reference);
        return ResponseEntity.ok(orderService.cancelOrder(
                java.util.UUID.fromString(order.getId().toString()), user));
    }

    @PostMapping("/{reference}/reorder")
    public ResponseEntity<CartDto> reorder(
            @AuthenticationPrincipal User user,
            @PathVariable String reference) {
        return ResponseEntity.ok(orderService.reorder(reference, user));
    }
}