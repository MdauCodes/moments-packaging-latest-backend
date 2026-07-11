package com.mdau.momentspackagingbackendjavafirstclient.payment.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.PaymentGatewayException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.notification.service.NotificationService;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.*;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderStatusHistoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.InvoiceNumberGenerator;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderReader;
import com.mdau.momentspackagingbackendjavafirstclient.payment.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.*;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.InventoryService;
import com.mdau.momentspackagingbackendjavafirstclient.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordRepository      paymentRecordRepository;
    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PayHeroService               payHeroService;
    private final DarajaService                darajaService;
    private final NotificationService          notificationService;
    private final OrderReader                  orderReader;
    private final CacheManager                 cacheManager;
    private final InventoryService             inventoryService;
    private final InvoiceNumberGenerator       invoiceNumberGenerator;
    private final ReferralService              referralService;

    // -- Initiate ---------------------------------------------------------

    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + request.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Order is not in PENDING_PAYMENT status");
        }

        Cache paymentCache = cacheManager.getCache("payment-idempotency");
        if (paymentCache != null) {
            Cache.ValueWrapper cached = paymentCache.get(order.getId().toString());
            if (cached != null) {
                String cachedStatus = (String) cached.get();
                log.info("Idempotent payment hit for order {} -- status: {}",
                        order.getReference(), cachedStatus);
                return buildStatusResponse(order, cachedStatus);
            }
        }

        List<PaymentRecord> existing = paymentRecordRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId());
        Instant twoMinutesAgo = Instant.now().minus(2, ChronoUnit.MINUTES);
        for (PaymentRecord prev : existing) {
            if ((prev.getStatus() == PaymentRecordStatus.PROCESSING ||
                 prev.getStatus() == PaymentRecordStatus.INITIATED) &&
                prev.getCreatedAt().isAfter(twoMinutesAgo)) {
                log.info("Blocking duplicate STK push for order {} -- existing record still PROCESSING",
                        order.getReference());
                if (paymentCache != null) {
                    paymentCache.put(order.getId().toString(), "PROCESSING");
                }
                return PaymentInitiateResponse.builder()
                        .orderId(order.getId())
                        .reference(order.getReference())
                        .status("STK_PUSH_SENT")
                        .message("A payment prompt was already sent to your phone. Please check and enter your PIN.")
                        .amount(order.getTotalAmount())
                        .build();
            }
        }

        for (PaymentRecord prev : existing) {
            if (prev.getStatus() == PaymentRecordStatus.INITIATED
                    || prev.getStatus() == PaymentRecordStatus.PROCESSING) {
                prev.setStatus(PaymentRecordStatus.CANCELLED);
                prev.setFailureReason("Superseded by new payment attempt");
                paymentRecordRepository.save(prev);
            }
        }

        PaymentMethod method = request.getPaymentMethod();

        // -- M-Pesa via Daraja --------------------------------------------
        if (method == PaymentMethod.MPESA) {
            if (request.getPhone() == null || request.getPhone().isBlank())
                throw new IllegalArgumentException("Phone is required for M-Pesa payment");

            String externalRef = UUID.randomUUID().toString();
            PaymentRecord record = PaymentRecord.builder()
                    .order(order).amount(order.getTotalAmount())
                    .method(method).status(PaymentRecordStatus.INITIATED)
                    .phone(request.getPhone()).externalReference(externalRef).build();
            paymentRecordRepository.save(record);

            try {
                String checkoutRequestId = darajaService.initiateSTKPush(
                        request.getPhone(), order.getTotalAmount(), externalRef);
                record.setCheckoutRequestId(checkoutRequestId);
                record.setStatus(PaymentRecordStatus.PROCESSING);
                paymentRecordRepository.save(record);

                if (paymentCache != null) {
                    paymentCache.put(order.getId().toString(), "PROCESSING");
                }
                return PaymentInitiateResponse.builder()
                        .orderId(order.getId())
                        .reference(order.getReference())
                        .status("STK_PUSH_SENT")
                        .message("Check your phone for M-Pesa prompt")
                        .amount(order.getTotalAmount())
                        .build();
            } catch (PaymentGatewayException e) {
                record.setStatus(PaymentRecordStatus.FAILED);
                record.setFailureReason(e.getMessage());
                paymentRecordRepository.save(record);
                throw e;
            } catch (Exception e) {
                record.setStatus(PaymentRecordStatus.FAILED);
                record.setFailureReason(e.getMessage());
                paymentRecordRepository.save(record);
                throw new RuntimeException("M-Pesa payment initiation failed: " + e.getMessage());
            }
        }

        // -- M-Pesa via Daraja (also handles legacy PAYHERO method) ----------
        if (method == PaymentMethod.PAYHERO) {
            if (request.getPhone() == null || request.getPhone().isBlank())
                throw new IllegalArgumentException("Phone is required for M-Pesa payment");

            String externalRef = UUID.randomUUID().toString();
            PaymentRecord record = PaymentRecord.builder()
                    .order(order).amount(order.getTotalAmount())
                    .method(PaymentMethod.MPESA).status(PaymentRecordStatus.INITIATED)
                    .phone(request.getPhone()).externalReference(externalRef).build();
            paymentRecordRepository.save(record);

            try {
                String checkoutRequestId = darajaService.initiateSTKPush(
                        request.getPhone(), order.getTotalAmount(), externalRef);
                record.setCheckoutRequestId(checkoutRequestId);
                record.setStatus(PaymentRecordStatus.PROCESSING);
                paymentRecordRepository.save(record);

                if (paymentCache != null) {
                    paymentCache.put(order.getId().toString(), "PROCESSING");
                }
                return PaymentInitiateResponse.builder()
                        .orderId(order.getId())
                        .reference(order.getReference())
                        .status("STK_PUSH_SENT")
                        .message("Check your phone for M-Pesa prompt")
                        .amount(order.getTotalAmount())
                        .build();
            } catch (PaymentGatewayException e) {
                record.setStatus(PaymentRecordStatus.FAILED);
                record.setFailureReason(e.getMessage());
                paymentRecordRepository.save(record);
                throw e;
            } catch (Exception e) {
                record.setStatus(PaymentRecordStatus.FAILED);
                record.setFailureReason(e.getMessage());
                paymentRecordRepository.save(record);
                throw new RuntimeException("Payment initiation failed: " + e.getMessage());
            }
        }

        if (method == PaymentMethod.BANK_TRANSFER) {
            paymentRecordRepository.save(PaymentRecord.builder()
                    .order(order).amount(order.getTotalAmount())
                    .method(method).status(PaymentRecordStatus.INITIATED).build());
            return PaymentInitiateResponse.builder()
                    .orderId(order.getId()).reference(order.getReference())
                    .status("BANK_TRANSFER_PENDING")
                    .message("Transfer KES " + order.getTotalAmount()
                            + " to our account. Use order ref: " + order.getReference())
                    .amount(order.getTotalAmount()).build();
        }

        paymentRecordRepository.save(PaymentRecord.builder()
                .order(order).amount(order.getTotalAmount())
                .method(method).status(PaymentRecordStatus.INITIATED).build());
        return PaymentInitiateResponse.builder()
                .orderId(order.getId()).reference(order.getReference())
                .status("COD_PENDING")
                .message("Pay KES " + order.getTotalAmount() + " on delivery")
                .amount(order.getTotalAmount()).build();
    }

    // -- PayHero Callback -------------------------------------------------

    @Transactional
    public void handleCallback(PayHeroCallbackDto callback) {
        if (callback.getResponse() == null) {
            log.error("PayHero callback has no response data");
            return;
        }
        PayHeroCallbackDto.PayHeroCallbackResponse response = callback.getResponse();
        processPaymentResult(
                response.getCheckoutRequestId(),
                response.getMerchantRequestId(),
                response.getResultCode(),
                response.getResultDesc(),
                response.getMpesaReceiptNumber(),
                "payhero-callback"
        );
    }

    // -- Daraja Callback --------------------------------------------------

    @Transactional
    public void handleDarajaCallback(DarajaCallbackDto callback) {
        if (callback.getBody() == null || callback.getBody().getStkCallback() == null) {
            log.error("Daraja callback has no stkCallback data");
            return;
        }
        DarajaCallbackDto.StkCallback stk = callback.getBody().getStkCallback();
        String receiptNumber = callback.getMetadataValue("MpesaReceiptNumber");

        processPaymentResult(
                stk.getCheckoutRequestId(),
                stk.getMerchantRequestId(),
                stk.getResultCode(),
                stk.getResultDesc(),
                receiptNumber,
                "daraja-callback"
        );
    }

    // -- Shared result processor ------------------------------------------

    private void processPaymentResult(String checkoutRequestId, String merchantRequestId,
                                      Integer resultCode, String resultDesc,
                                      String receiptNumber, String changedBy) {
        PaymentRecord record = paymentRecordRepository
                .findByCheckoutRequestId(checkoutRequestId).orElse(null);
        if (record == null) {
            log.warn("No payment record found for checkoutRequestId: {}", checkoutRequestId);
            return;
        }
        if (record.getStatus() == PaymentRecordStatus.SUCCESS) {
            log.warn("Duplicate callback for already-completed payment {}. Ignoring.", record.getId());
            return;
        }
        if (record.getStatus() == PaymentRecordStatus.CANCELLED) {
            log.warn("Callback for cancelled record {}. Ignoring.", record.getId());
            return;
        }

        record.setMerchantRequestId(merchantRequestId);
        boolean isSuccess = resultCode != null && resultCode == 0;

        if (isSuccess) {
            record.setStatus(PaymentRecordStatus.SUCCESS);
            record.setReceiptNumber(receiptNumber);
            paymentRecordRepository.save(record);

            Order order = record.getOrder();
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.PAID);
            if (order.getInvoiceNumber() == null) {
                order.setInvoiceNumber(invoiceNumberGenerator.generate());
                order.setPaidAt(Instant.now());
            }
            orderRepository.save(order);

            if (order.getCustomer() != null) {
                try {
                    referralService.processOrderForReferral(order.getCustomer(), order);
                    referralService.awardOrderPoints(order.getCustomer(), order);
                } catch (Exception e) {
                    log.error("Rewards processing failed for order {}: {}",
                            order.getReference(), e.getMessage(), e);
                }
            }

            historyRepository.save(OrderStatusHistory.builder()
                    .order(order).fromStatus(OrderStatus.PENDING_PAYMENT)
                    .toStatus(OrderStatus.PAID)
                    .note("Payment received. Receipt: " + receiptNumber)
                    .changedBy(changedBy).build());

            Cache paymentCache = cacheManager.getCache("payment-idempotency");
            if (paymentCache != null) paymentCache.evict(order.getId().toString());

            try {
                inventoryService.deductForOrder(order);
            } catch (Exception e) {
                log.error("Stock deduction failed for order {} after payment: {}",
                        order.getReference(), e.getMessage(), e);
            }

            Order paidOrder = orderReader.loadFresh(order.getId());
            notificationService.onOrderPaid(paidOrder);
            log.info("Payment SUCCESS [{}] for order {}, receipt={}",
                    changedBy, order.getReference(), receiptNumber);

        } else {
            record.setStatus(PaymentRecordStatus.FAILED);
            record.setFailureReason(resultDesc);
            paymentRecordRepository.save(record);

            Order order = record.getOrder();
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            Cache paymentCache = cacheManager.getCache("payment-idempotency");
            if (paymentCache != null) paymentCache.evict(order.getId().toString());

            notificationService.onPaymentFailed(
                    orderReader.loadFresh(order.getId()), resultDesc);
            log.info("Payment FAILED [{}] for order {}: {}",
                    changedBy, order.getReference(), resultDesc);
        }
    }

    // -- Status -----------------------------------------------------------

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<PaymentRecord> records = paymentRecordRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId);

        if (records.isEmpty()) {
            return PaymentStatusResponse.builder()
                    .orderId(order.getId())
                    .orderReference(order.getReference())
                    .status("NO_PAYMENT")
                    .amount(order.getTotalAmount())
                    .build();
        }

        PaymentRecord latest = records.get(0);
        String normalizedStatus = normalizeStatus(latest.getStatus());

        return PaymentStatusResponse.builder()
                .orderId(order.getId())
                .orderReference(order.getReference())
                .status(normalizedStatus)
                .amount(order.getTotalAmount())
                .receiptNumber(latest.getReceiptNumber())
                .failureReason(latest.getFailureReason())
                .paymentMethod(latest.getMethod() != null ? latest.getMethod().name() : null)
                .message(buildMessage(normalizedStatus, latest))
                .build();
    }

    // -- Private helpers --------------------------------------------------

    private PaymentInitiateResponse buildStatusResponse(Order order, String cachedStatus) {
        String message = "PROCESSING".equals(cachedStatus)
                ? "A payment prompt was already sent to your phone. Please check and enter your PIN."
                : "Payment already completed for this order.";
        return PaymentInitiateResponse.builder()
                .orderId(order.getId())
                .reference(order.getReference())
                .status("STK_PUSH_SENT")
                .message(message)
                .amount(order.getTotalAmount())
                .build();
    }

    private String normalizeStatus(PaymentRecordStatus status) {
        return switch (status) {
            case SUCCESS                -> "SUCCESS";
            case FAILED, CANCELLED     -> "FAILED";
            case INITIATED, PROCESSING -> "PROCESSING";
        };
    }

    private String buildMessage(String normalizedStatus, PaymentRecord record) {
        return switch (normalizedStatus) {
            case "SUCCESS"    -> "Payment confirmed. Receipt: " + record.getReceiptNumber();
            case "FAILED"     -> record.getFailureReason() != null
                    ? record.getFailureReason() : "Payment was not completed.";
            case "PROCESSING" -> "Waiting for M-Pesa confirmation. Enter your PIN on your phone.";
            default           -> "No payment initiated yet.";
        };
    }
}