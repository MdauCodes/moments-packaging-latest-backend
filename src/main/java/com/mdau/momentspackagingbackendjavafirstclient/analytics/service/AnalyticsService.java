package com.mdau.momentspackagingbackendjavafirstclient.analytics.service;

import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.OperationsSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.PaymentMethodBreakdownDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.RevenueSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.StatusCountDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.StatusDurationDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderStatusHistoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecordStatus;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Backs the comprehensive analytics dashboard — built phase by phase (see project notes).
 * Phase 1: revenue and payment health for an explicit date range. Every method here takes an
 * explicit [start, end) range rather than a fixed window, so the frontend's date-range picker
 * (day/week/month/year/custom) is the only thing that decides what period is shown.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository              orderRepository;
    private final PaymentRecordRepository      paymentRecordRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional(readOnly = true)
    public RevenueSummaryDto getRevenueSummary(Instant start, Instant end) {
        BigDecimal paidRevenue = orNil(orderRepository.sumOrdersInRangeByPaymentStatus(start, end, PaymentStatus.PAID));
        long paidCount = orderRepository.countOrdersInRangeByPaymentStatus(start, end, PaymentStatus.PAID);

        BigDecimal pendingValue = orNil(orderRepository.sumOrdersInRangeByPaymentStatus(start, end, PaymentStatus.PENDING));
        long pendingCount = orderRepository.countOrdersInRangeByPaymentStatus(start, end, PaymentStatus.PENDING);

        BigDecimal failedValue = orNil(orderRepository.sumOrdersInRangeByPaymentStatus(start, end, PaymentStatus.FAILED));
        long failedCount = orderRepository.countOrdersInRangeByPaymentStatus(start, end, PaymentStatus.FAILED);

        BigDecimal refundedValue = orNil(orderRepository.sumOrdersInRangeByPaymentStatus(start, end, PaymentStatus.REFUNDED));
        long refundedCount = orderRepository.countOrdersInRangeByPaymentStatus(start, end, PaymentStatus.REFUNDED);

        BigDecimal avgOrderValue = paidCount > 0
                ? paidRevenue.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<PaymentMethodBreakdownDto> byMethod = buildMethodBreakdown(start, end);

        return new RevenueSummaryDto(
                start, end,
                paidRevenue, paidCount,
                pendingValue, pendingCount,
                failedValue, failedCount,
                refundedValue, refundedCount,
                avgOrderValue,
                byMethod
        );
    }

    @Transactional(readOnly = true)
    public OperationsSummaryDto getOperationsSummary(Instant start, Instant end) {
        // ── Funnel ──────────────────────────────────────────────────────────
        List<Object[]> statusRows = orderRepository.countByStatusInRange(start, end);
        List<StatusCountDto> funnel = new ArrayList<>();
        long totalOrders = 0;
        long cancelledOrders = 0;
        for (Object[] row : statusRows) {
            OrderStatus status = (OrderStatus) row[0];
            long count = ((Number) row[1]).longValue();
            funnel.add(new StatusCountDto(status.name(), count));
            totalOrders += count;
            if (status == OrderStatus.CANCELLED) cancelledOrders = count;
        }
        double cancellationRate = totalOrders > 0 ? round1(cancelledOrders * 100.0 / totalOrders) : 0.0;

        // ── Average time in stage — completed transitions only ─────────────
        List<StatusDurationDto> avgTimeInStage = buildAvgTimeInStage(start, end);

        // ── Repeat customers ─────────────────────────────────────────────────
        List<UUID> customerIds = orderRepository.findDistinctCustomerIdsInRange(start, end);
        long distinctCustomerCount = customerIds.size();
        long repeatCustomerCount = 0;
        if (!customerIds.isEmpty()) {
            List<Object[]> lifetimeCounts = orderRepository.countLifetimePaidOrdersByCustomerIds(customerIds);
            for (Object[] row : lifetimeCounts) {
                long lifetimePaid = ((Number) row[1]).longValue();
                if (lifetimePaid >= 2) repeatCustomerCount++;
            }
        }
        double repeatRate = distinctCustomerCount > 0 ? round1(repeatCustomerCount * 100.0 / distinctCustomerCount) : 0.0;

        // ── Refunds ──────────────────────────────────────────────────────────
        List<Object[]> refundRequested = orderRepository.refundsRequestedInRange(start, end);
        long refundRequestedCount = 0;
        BigDecimal refundRequestedValue = BigDecimal.ZERO;
        if (!refundRequested.isEmpty()) {
            Object[] row = refundRequested.get(0);
            refundRequestedCount = ((Number) row[0]).longValue();
            refundRequestedValue = orNil((BigDecimal) row[1]);
        }

        List<Order> resolvedRefunds = orderRepository.findResolvedRefundsInRange(start, end);
        double avgResolutionHours = 0.0;
        if (!resolvedRefunds.isEmpty()) {
            double totalHours = 0;
            for (Order o : resolvedRefunds) {
                totalHours += Duration.between(o.getRefundRequestedAt(), o.getRefundResolvedAt()).toMinutes() / 60.0;
            }
            avgResolutionHours = round1(totalHours / resolvedRefunds.size());
        }

        return new OperationsSummaryDto(
                start, end,
                totalOrders, funnel, avgTimeInStage,
                cancelledOrders, cancellationRate,
                distinctCustomerCount, repeatCustomerCount, repeatRate,
                refundRequestedCount, refundRequestedValue, resolvedRefunds.size(), avgResolutionHours
        );
    }

    /** Turns consecutive changedAt deltas per order into an average "time spent in stage X",
     *  counting only completed transitions (an order's current/last stage has no "next" row yet). */
    private List<StatusDurationDto> buildAvgTimeInStage(Instant start, Instant end) {
        List<Object[]> rows = orderStatusHistoryRepository.findForOrdersCreatedInRange(start, end);

        Map<String, double[]> tally = new LinkedHashMap<>(); // status -> [totalHours, count]
        UUID prevOrderId = null;
        OrderStatus prevStatus = null;
        Instant prevChangedAt = null;

        for (Object[] row : rows) {
            UUID orderId = (UUID) row[0];
            OrderStatus toStatus = (OrderStatus) row[1];
            Instant changedAt = (Instant) row[2];

            if (orderId.equals(prevOrderId)) {
                double hours = Duration.between(prevChangedAt, changedAt).toMinutes() / 60.0;
                double[] bucket = tally.computeIfAbsent(prevStatus.name(), k -> new double[2]);
                bucket[0] += hours;
                bucket[1] += 1;
            }
            prevOrderId = orderId;
            prevStatus = toStatus;
            prevChangedAt = changedAt;
        }

        return tally.entrySet().stream()
                .map(e -> new StatusDurationDto(e.getKey(), round1(e.getValue()[0] / e.getValue()[1]), (long) e.getValue()[1]))
                .toList();
    }

    private List<PaymentMethodBreakdownDto> buildMethodBreakdown(Instant start, Instant end) {
        // [0]=method (enum), [1]=status (enum), [2]=count (Long) — grouped per attempt, not per order,
        // since one order can carry several payment attempts (e.g. an M-Pesa STK retry).
        List<Object[]> rows = paymentRecordRepository.countByMethodAndStatusInRange(start, end);

        Map<String, long[]> tally = new LinkedHashMap<>(); // method -> [success, failed, other]
        for (Object[] row : rows) {
            String method = String.valueOf(row[0]);
            PaymentRecordStatus status = (PaymentRecordStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long[] bucket = tally.computeIfAbsent(method, k -> new long[3]);
            if (status == PaymentRecordStatus.SUCCESS) bucket[0] += count;
            else if (status == PaymentRecordStatus.FAILED || status == PaymentRecordStatus.CANCELLED) bucket[1] += count;
            else bucket[2] += count;
        }

        return tally.entrySet().stream()
                .map(e -> {
                    long success = e.getValue()[0];
                    long failed = e.getValue()[1];
                    long other = e.getValue()[2];
                    long decided = success + failed;
                    double rate = decided > 0 ? (success * 100.0 / decided) : 0.0;
                    return new PaymentMethodBreakdownDto(e.getKey(), success, failed, other, round1(rate));
                })
                .toList();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static BigDecimal orNil(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
