package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderTrackingDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService orderService;

    @GetMapping("/track/{reference}")
    public ResponseEntity<OrderTrackingDto> track(@PathVariable String reference) {
        return ResponseEntity.ok(orderService.getTrackingInfo(reference));
    }
}