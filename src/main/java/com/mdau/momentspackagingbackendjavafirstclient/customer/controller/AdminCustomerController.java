package com.mdau.momentspackagingbackendjavafirstclient.customer.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.customer.dto.CustomerDto;
import com.mdau.momentspackagingbackendjavafirstclient.customer.service.AdminCustomerService;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AdminCustomerService customerService;
    private final OrderService orderService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<CustomerDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String segment,
            @PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(customerService.list(q, status, segment, pageable));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        CustomerDto customer = customerService.getById(id);
        PageResponse<OrderDto> orders = orderService.getAllOrders(
                null, id, PageRequest.of(0, 50, Sort.by("createdAt").descending()));

        Map<String, Object> response = new HashMap<>();
        response.put("customer", customer);
        response.put("orders", orders.getContent());
        return ResponseEntity.ok(response);
    }
}
