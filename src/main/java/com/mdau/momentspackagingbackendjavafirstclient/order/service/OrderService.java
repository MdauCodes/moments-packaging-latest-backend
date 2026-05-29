package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.cart.dto.CartDto;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartItemRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.service.CartService;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.notification.service.NotificationService;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.*;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderStatusHistoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecord;
import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecordStatus;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.InventoryService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderReader                  orderReader;
    private final CartService                  cartService;
    private final CartRepository               cartRepository;
    private final CartItemRepository           cartItemRepository;
    private final NotificationService          notificationService;
    private final PaymentRecordRepository      paymentRecordRepository;
    private final InventoryService             inventoryService;

    // -- Queries ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderDto getByReference(String reference) {
        return orderRepository.findByReference(reference)
                .map(OrderDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + reference));
    }

    @Transactional(readOnly = true)
    public OrderDto getById(UUID id) {
        return orderRepository.findById(id)
                .map(OrderDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public OrderTrackingDto getTrackingInfo(String reference) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + reference));
        return new OrderTrackingDto(
                order.getId(),
                order.getReference(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getPaymentMethod().name(),
                order.getContactName(),
                maskEmail(order.getEmail()),
                order.getItems().stream()
                        .map(i -> new OrderTrackingDto.TrackingItemDto(
                                i.getProductNameSnapshot(), i.getQuantity(), i.getLineTotal()))
                        .toList(),
                order.getStatusHistory().stream()
                        .map(h -> new OrderTrackingDto.TrackingHistoryDto(
                                h.getToStatus().name(), h.getChangedAt()))
                        .toList(),
                order.getTotalAmount(),
                order.getDeliveryFee(),
                order.getFulfillmentType() != null ? order.getFulfillmentType().name() : null);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> getMyOrders(User customer, Pageable pageable) {
        return new PageResponse<>(
                orderRepository.findByCustomerOrderByCreatedAtDesc(customer, pageable)
                        .map(OrderDto::new));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> getAllOrders(OrderStatus status, UUID customerId,
                                               Pageable pageable) {
        return new PageResponse<>(
                orderRepository.findAllWithFilters(status, customerId, pageable)
                        .map(OrderDto::new));
    }

    // -- Commands ---------------------------------------------------------------

    @Transactional
    public OrderDto updateStatus(UUID id, String newStatusStr,
                                 String staffNotes, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus newStatus = OrderStatus.valueOf(newStatusStr);
        OrderStatus oldStatus = order.getStatus();

        order.setStatus(newStatus);
        if (staffNotes != null) order.setStaffNotes(staffNotes);
        orderRepository.save(order);

        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .note(staffNotes)
                .changedBy(changedBy)
                .build());

        Order fresh = orderReader.loadFresh(id);

        switch (newStatus) {
            case IN_PRODUCTION      -> notificationService.onOrderInProduction(fresh);
            case READY_FOR_DISPATCH -> notificationService.onOrderReadyForDispatch(fresh);
            case DISPATCHED         -> notificationService.onOrderDispatched(fresh);
            case DELIVERED          -> notificationService.onOrderDelivered(fresh);
            case CANCELLED          -> notificationService.onOrderCancelled(fresh);
            default -> {}
        }

        return new OrderDto(fresh);
    }

    @Transactional
    public OrderDto dispatchConfirm(UUID id, String deliveryConfirmationStatusStr,
                                    Boolean contentsVerified, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.DISPATCHED) {
            throw new IllegalArgumentException("Order has already been dispatched");
        }

        if (contentsVerified != null) {
            order.setContentsVerified(contentsVerified);
        }

        if (deliveryConfirmationStatusStr != null) {
            order.setDeliveryConfirmationStatus(
                    DeliveryConfirmationStatus.valueOf(deliveryConfirmationStatusStr));
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DISPATCHED);
        orderRepository.save(order);

        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(oldStatus)
                .toStatus(OrderStatus.DISPATCHED)
                .note("Dispatch confirmed — contents verified: " + Boolean.TRUE.equals(contentsVerified))
                .changedBy(changedBy)
                .build());

        Order fresh = orderReader.loadFresh(id);
        notificationService.onOrderDispatched(fresh);
        return new OrderDto(fresh);
    }

    @Transactional
    public OrderDto processRefund(UUID id, String reason, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus old = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStaffNotes(reason);
        orderRepository.save(order);

        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(old)
                .toStatus(OrderStatus.REFUNDED)
                .note(reason)
                .changedBy(changedBy)
                .build());

        List<PaymentRecord> records = paymentRecordRepository
                .findByOrderIdOrderByCreatedAtDesc(id);
        if (!records.isEmpty()) {
            PaymentRecord latest = records.get(0);
            latest.setStatus(PaymentRecordStatus.FAILED);
            latest.setFailureReason("Refunded: " + reason);
            paymentRecordRepository.save(latest);
        }

        try {
            inventoryService.restoreForOrder(order);
        } catch (Exception e) {
            log.error("Stock restore failed for refunded order {}: {}",
                    order.getReference(), e.getMessage(), e);
        }

        Order fresh = orderReader.loadFresh(id);
        notificationService.onOrderCancelled(fresh);
        return new OrderDto(fresh);
    }

    @Transactional
    public OrderDto cancelOrder(UUID id, User customer) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getCustomer() == null ||
                !order.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Order not found");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT &&
                order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Order cannot be cancelled at this stage");
        }

        OrderStatus old = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        if (old == OrderStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            try {
                inventoryService.restoreForOrder(order);
            } catch (Exception e) {
                log.error("Stock restore failed for cancelled order {}: {}",
                        order.getReference(), e.getMessage(), e);
            }
        }
        orderRepository.save(order);

        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(old)
                .toStatus(OrderStatus.CANCELLED)
                .note("Cancelled by customer")
                .changedBy(customer.getEmail())
                .build());

        Order fresh = orderReader.loadFresh(id);
        notificationService.onOrderCancelled(fresh);
        return new OrderDto(fresh);
    }

    @Transactional
    public OrderDto assignOrder(UUID id, String assignedTo) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setAssignedTo(assignedTo);
        orderRepository.save(order);
        return orderReader.loadFreshDto(id);
    }

    @Transactional
    public CartDto reorder(String reference, User customer) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.getItems().forEach(item -> {
            com.mdau.momentspackagingbackendjavafirstclient.cart.dto.AddToCartRequest req =
                    new com.mdau.momentspackagingbackendjavafirstclient.cart.dto.AddToCartRequest();
            req.setProductId(item.getProductId());
            req.setQuantity(item.getQuantity());
            req.setSize(item.getSizeSnapshot());
            req.setMaterial(item.getMaterialSnapshot());
            req.setFinish(item.getFinishSnapshot());
            cartService.addItem(customer, null, req);
        });

        return cartService.getCart(customer, null);
    }

    // -- Helpers ----------------------------------------------------------------

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts  = email.split("@");
        String local    = parts[0];
        String visible  = local.length() > 2 ? local.substring(0, 2) : local.substring(0, 1);
        return visible + "***@" + parts[1];
    }
}