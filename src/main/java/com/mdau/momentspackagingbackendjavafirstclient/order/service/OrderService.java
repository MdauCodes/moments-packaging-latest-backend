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
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.PaymentService;
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
    private final PaymentService               paymentService;

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

    /**
     * @param email Optional — when it matches the order's own email (case-insensitive), the
     *              full record (financials, contact name, items, delivery address) is returned.
     *              Otherwise only status/tracking-progress fields are — order references are
     *              sequential and guessable, so reference-alone must never expose PII/financials.
     */
    @Transactional(readOnly = true)
    public OrderTrackingDto getTrackingInfo(String reference, String email) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + reference));

        boolean verified = email != null && !email.isBlank()
                && order.getEmail() != null
                && order.getEmail().trim().equalsIgnoreCase(email.trim());

        List<OrderTrackingDto.TrackingHistoryDto> history = order.getStatusHistory().stream()
                .map(h -> new OrderTrackingDto.TrackingHistoryDto(h.getToStatus().name(), h.getChangedAt()))
                .toList();

        if (!verified) {
            return new OrderTrackingDto(
                    false,
                    order.getId(),
                    order.getReference(),
                    order.getStatus().name(),
                    order.getPaymentStatus().name(),
                    order.getPaymentMethod().name(),
                    null,                       // contactName
                    maskEmail(order.getEmail()),
                    List.of(),                  // items
                    history,
                    null, null,                 // totalAmount, deliveryFee
                    order.getFulfillmentType() != null ? order.getFulfillmentType().name() : null,
                    null, null, null, null,     // subtotal, discount, vatAmount, invoiceNumber
                    order.getCreatedAt(),
                    null,                       // paidAt
                    null,                       // taxInvoiceKraPin
                    null, null);                // deliveryAddress, county
        }

        return new OrderTrackingDto(
                true,
                order.getId(),
                order.getReference(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getPaymentMethod().name(),
                order.getContactName(),
                maskEmail(order.getEmail()),
                order.getItems().stream()
                        .map(i -> new OrderTrackingDto.TrackingItemDto(
                                i.getProductNameSnapshot(), i.getQuantity(), i.getLineTotal(),
                                i.getUnitPrice(), i.getSizeSnapshot(), i.getMaterialSnapshot(), i.getFinishSnapshot()))
                        .toList(),
                history,
                order.getTotalAmount(),
                order.getDeliveryFee(),
                order.getFulfillmentType() != null ? order.getFulfillmentType().name() : null,
                order.getSubtotal(),
                order.getDiscount(),
                order.getVatAmount(),
                order.getInvoiceNumber(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getTaxInvoiceKraPin(),
                order.getDeliveryAddress(),
                order.getCounty());
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

        if (staffNotes != null) {
            order.setStaffNotes(staffNotes);
            orderRepository.save(order);
        }

        // A manual transition to PAID (bank transfer / COD, confirmed by staff) must get
        // the exact same treatment as an M-Pesa callback — referral payout, order points,
        // inventory deduction, invoice number, paid notification — not just a bare status
        // flip. This used to silently skip all of that for non-M-Pesa payment methods.
        if (newStatus == OrderStatus.PAID && order.getPaymentStatus() != PaymentStatus.PAID) {
            paymentService.markOrderPaidManually(id, null, changedBy);
            return new OrderDto(orderReader.loadFresh(id));
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
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

    /**
     * Logs a refund request as a complaint for an admin to review — deliberately does NOT touch
     * order status, payment status, payment records, or inventory. Those are separate, explicit
     * admin actions (see markPaymentFailed / restoreInventory / manual status override) so a
     * refund never happens as an automatic side effect of someone just flagging that one might
     * be needed.
     */
    @Transactional
    public OrderDto requestRefund(UUID id, String reason, String requestedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setRefundRequestedAt(java.time.Instant.now());
        order.setRefundRequestReason(reason);
        order.setRefundRequestedBy(requestedBy);
        order.setRefundResolvedAt(null);
        orderRepository.save(order);

        log.info("Refund requested for order {} by {}: {}", order.getReference(), requestedBy, reason);
        return new OrderDto(order);
    }

    /** Clears an order's open refund request once an admin has finished handling it manually. */
    @Transactional
    public OrderDto resolveRefundRequest(UUID id, String resolvedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setRefundResolvedAt(java.time.Instant.now());
        orderRepository.save(order);
        log.info("Refund request resolved for order {} by {}", order.getReference(), resolvedBy);
        return new OrderDto(order);
    }

    /**
     * Explicit, standalone admin action: marks the order's latest payment record as failed/refunded.
     * Never triggered automatically — an admin decides this only once they've actually processed
     * the real-world refund (e.g. a manual M-Pesa reversal) themselves.
     */
    @Transactional
    public OrderDto markPaymentFailed(UUID id, String reason, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<PaymentRecord> records = paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(id);
        if (!records.isEmpty()) {
            PaymentRecord latest = records.get(0);
            latest.setStatus(PaymentRecordStatus.FAILED);
            latest.setFailureReason("Refunded: " + reason);
            paymentRecordRepository.save(latest);
        }
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStaffNotes(reason);
        orderRepository.save(order);

        log.info("Payment marked refunded/failed for order {} by {}: {}", order.getReference(), changedBy, reason);
        return new OrderDto(orderReader.loadFresh(id));
    }

    /**
     * Explicit, standalone admin action: restores stock for every item on this order. Never
     * triggered automatically alongside a status change — an admin clicks this only when they've
     * confirmed the goods are actually coming back into inventory.
     */
    @Transactional
    public OrderDto restoreInventory(UUID id, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        inventoryService.restoreForOrder(order);
        log.info("Inventory restored for order {} by {}", order.getReference(), changedBy);
        return new OrderDto(orderReader.loadFresh(id));
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