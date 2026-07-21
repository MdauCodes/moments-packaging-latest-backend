package com.mdau.momentspackagingbackendjavafirstclient.analytics.service;

import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.PaymentMethodBreakdownDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.RevenueSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecordStatus;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backs the comprehensive analytics dashboard — built phase by phase (see project notes).
 * Phase 1: revenue and payment health for an explicit date range. Every method here takes an
 * explicit [start, end) range rather than a fixed window, so the frontend's date-range picker
 * (day/week/month/year/custom) is the only thing that decides what period is shown.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository         orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;

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
