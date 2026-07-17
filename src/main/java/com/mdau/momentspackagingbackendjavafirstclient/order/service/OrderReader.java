package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Loads an Order in a brand-new transaction so the caller receives an entity
 * that is completely detached from the previous session's L1 cache.
 * Use this after a write transaction commits to build a clean DTO or pass
 * to async notification methods without Hibernate queued-ops conflicts.
 */
@Service
@RequiredArgsConstructor
public class OrderReader {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order loadFresh(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        // Force-initialize lazy collections while session is open
        order.getItems().size();
        order.getStatusHistory().size();
        // Hibernate special-cases the @Id getter on a lazy proxy to avoid a DB round-trip, so
        // calling .getId() alone does NOT initialize it — must use Hibernate.initialize() directly,
        // or the proxy is still unresolved once this leaves the transaction (e.g. for async emails).
        if (order.getCustomer() != null) {
            Hibernate.initialize(order.getCustomer());
        }
        return order;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDto loadFreshDto(UUID id) {
        return new OrderDto(loadFresh(id));
    }
}