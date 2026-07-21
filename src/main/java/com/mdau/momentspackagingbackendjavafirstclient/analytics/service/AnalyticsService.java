package com.mdau.momentspackagingbackendjavafirstclient.analytics.service;

import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.OperationsSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.PaymentMethodBreakdownDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.ProductPerformanceDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.ProductsInventoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.RevenueSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.RewardsEconomicsDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.RewardsSourceBreakdownDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.StatusCountDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.StatusDurationDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.StockAlertDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.TaxReportDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.TopWalletHolderDto;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundleStatus;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.repository.DocumentBundleRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderStatusHistoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecordStatus;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditTransactionType;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralEventStatus;
import com.mdau.momentspackagingbackendjavafirstclient.referral.repository.CreditTransactionRepository;
import com.mdau.momentspackagingbackendjavafirstclient.referral.repository.CreditWalletRepository;
import com.mdau.momentspackagingbackendjavafirstclient.referral.repository.ReferralEventRepository;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    private static final String KEY_CREDITS_PER_KES = "referral.credits.per.kes";

    private final OrderRepository              orderRepository;
    private final PaymentRecordRepository      paymentRecordRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CreditTransactionRepository  creditTransactionRepository;
    private final CreditWalletRepository       creditWalletRepository;
    private final ReferralEventRepository      referralEventRepository;
    private final SettingsService              settingsService;
    private final DocumentBundleRepository     documentBundleRepository;
    private final ProductRepository            productRepository;

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

    /**
     * Phase 3 — Reward Coupons & referral economics. Coupon counts are converted to KES using the
     * same `referral.credits.per.kes` setting and rounding (FLOOR, 2dp) that ReferralService itself
     * uses at redemption time, so the dashboard figures always agree with what customers actually see.
     */
    @Transactional(readOnly = true)
    public RewardsEconomicsDto getRewardsEconomics(Instant start, Instant end) {
        int creditsPerKes = Integer.parseInt(settingsService.getValue(KEY_CREDITS_PER_KES, "10"));

        long outstandingBalance = creditWalletRepository.sumAllBalances();
        BigDecimal outstandingValueKes = creditsToKes(outstandingBalance, creditsPerKes);

        List<Integer> allBalances = creditWalletRepository.findAllBalances();
        double medianBalance = median(allBalances);

        List<TopWalletHolderDto> topHolders = creditWalletRepository.findTopHolders(PageRequest.of(0, 10)).stream()
                .map(row -> {
                    String name = String.valueOf(row[0]) + " " + String.valueOf(row[1]);
                    int balance = ((Number) row[2]).intValue();
                    return new TopWalletHolderDto(name.trim(), balance, creditsToKes(balance, creditsPerKes));
                })
                .toList();

        List<Object[]> typeRows = creditTransactionRepository.sumByTypeInRange(start, end);
        List<RewardsSourceBreakdownDto> earnedInRange = new ArrayList<>();
        long redeemedCoupons = 0;
        for (Object[] row : typeRows) {
            CreditTransactionType type = (CreditTransactionType) row[0];
            long coupons = ((Number) row[2]).longValue();
            if (type == CreditTransactionType.REDEEMED) {
                redeemedCoupons = coupons;
            } else if (type.name().startsWith("EARNED_")) {
                earnedInRange.add(new RewardsSourceBreakdownDto(type.name(), coupons, creditsToKes(coupons, creditsPerKes)));
            }
        }
        BigDecimal redeemedValueKes = creditsToKes(redeemedCoupons, creditsPerKes);

        // Referral conversion — of referrals whose referee signed up in this range, how many confirmed.
        List<Object[]> statusRows = referralEventRepository.countByStatusInRange(start, end);
        long referralSignups = 0;
        long referralConfirmed = 0;
        for (Object[] row : statusRows) {
            ReferralEventStatus status = (ReferralEventStatus) row[0];
            long count = ((Number) row[1]).longValue();
            referralSignups += count;
            if (status == ReferralEventStatus.CONFIRMED) referralConfirmed = count;
        }
        double conversionRate = referralSignups > 0 ? round1(referralConfirmed * 100.0 / referralSignups) : 0.0;

        return new RewardsEconomicsDto(
                start, end,
                outstandingBalance, outstandingValueKes,
                redeemedCoupons, redeemedValueKes,
                earnedInRange,
                conversionRate, referralSignups, referralConfirmed,
                redeemedValueKes,
                medianBalance,
                topHolders
        );
    }

    /**
     * Phase 4 — tax reporting. VAT is only ever due on what a paying customer actually paid, so
     * this only ever sums PAID orders (same discipline as revenue in Phase 1) using the already-
     * corrected post-discount taxableAmount/vatAmount fields (see Order.java — the VAT-on-discount
     * fix). Also surfaces tax-document compliance: how many paid orders asked for a tax invoice /
     * ETR, and where each requested ETR bundle currently sits in its PENDING→SENT/FAILED lifecycle.
     */
    @Transactional(readOnly = true)
    public TaxReportDto getTaxReport(Instant start, Instant end) {
        List<Object[]> vatRows = orderRepository.sumVatForPaidInRange(start, end);
        Object[] vatRow = vatRows.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L} : vatRows.get(0);
        BigDecimal vatableSales = orNil((BigDecimal) vatRow[0]);
        BigDecimal vatToRemit = orNil((BigDecimal) vatRow[1]);
        long paidOrderCount = ((Number) vatRow[2]).longValue();

        long taxInvoiceRequested = orderRepository.countTaxInvoiceRequestedInRange(start, end);
        long etrRequested = orderRepository.countEtrRequestedInRange(start, end);

        List<StatusCountDto> bundleCounts = documentBundleRepository.countByStatusForOrdersInRange(start, end).stream()
                .map(row -> new StatusCountDto(((DocumentBundleStatus) row[0]).name(), ((Number) row[1]).longValue()))
                .toList();

        return new TaxReportDto(
                start, end,
                vatableSales, vatToRemit, paidOrderCount,
                taxInvoiceRequested, etrRequested,
                bundleCounts
        );
    }

    /**
     * Phase 5 — products & inventory. Top sellers are date-ranged (PAID orders only, same
     * discipline as every other revenue figure here); stock levels and inventory valuation are a
     * live snapshot, not date-scoped, since "how much stock do we hold right now" doesn't have a
     * meaningful historical range. costPrice is only populated for Riseller-synced items, so the
     * cost-based valuation is a floor, not a guarantee — productsMissingCostPriceCount says how big
     * that gap is rather than silently under-reporting it.
     */
    @Transactional(readOnly = true)
    public ProductsInventoryDto getProductsInventory(Instant start, Instant end) {
        List<ProductPerformanceDto> topSelling = orderRepository.findTopSellingProductsInRange(start, end, PageRequest.of(0, 10)).stream()
                .map(row -> new ProductPerformanceDto(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue(),
                        orNil((BigDecimal) row[2])))
                .toList();

        long inStockCount = productRepository.countByStockStatusAndDeletedFalse(StockStatus.IN_STOCK);
        long lowStockCount = productRepository.countByStockStatusAndDeletedFalse(StockStatus.LOW_STOCK);
        long outOfStockCount = productRepository.countByStockStatusAndDeletedFalse(StockStatus.OUT_OF_STOCK);

        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;
        long missingCostPriceCount = 0;
        for (Object[] row : productRepository.findInventoryValuationRows()) {
            BigDecimal costPrice = (BigDecimal) row[0];
            BigDecimal basePrice = (BigDecimal) row[1];
            int stockCount = ((Number) row[2]).intValue();
            if (costPrice == null) {
                missingCostPriceCount++;
            } else {
                totalCostValue = totalCostValue.add(costPrice.multiply(BigDecimal.valueOf(stockCount)));
            }
            if (basePrice != null) {
                totalRetailValue = totalRetailValue.add(basePrice.multiply(BigDecimal.valueOf(stockCount)));
            }
        }

        List<StockAlertDto> lowStockAlerts = productRepository.findLowStockAlerts(PageRequest.of(0, 10)).stream()
                .map(p -> new StockAlertDto(p.getName(), nz(p.getStockCount()), nz(p.getLowStockThreshold()), p.getStockStatus().name()))
                .toList();

        return new ProductsInventoryDto(
                start, end,
                topSelling,
                inStockCount, lowStockCount, outOfStockCount,
                totalCostValue.setScale(2, RoundingMode.HALF_UP),
                totalRetailValue.setScale(2, RoundingMode.HALF_UP),
                missingCostPriceCount,
                lowStockAlerts
        );
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static double median(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(null);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? (sorted.get(mid - 1) + sorted.get(mid)) / 2.0
                : sorted.get(mid);
    }

    private static BigDecimal creditsToKes(long credits, int creditsPerKes) {
        return BigDecimal.valueOf(credits).divide(BigDecimal.valueOf(creditsPerKes), 2, RoundingMode.FLOOR);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static BigDecimal orNil(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
