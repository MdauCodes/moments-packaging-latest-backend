package com.mdau.momentspackagingbackendjavafirstclient.settings.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.lead.repository.LeadRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analytics Phase 9 note: the comprehensive date-ranged dashboard (AdminAnalyticsRevenueController)
 * now covers revenue, operations, rewards, tax, products, profitability, projections and customers
 * with a consistent PAID-only revenue definition. This controller's fixed-window /overview endpoint
 * is kept as-is — it's still the only source for total products/users/enquiries/leads and today/
 * week/MTD figures the frontend's legacy "operational snapshot" panel displays — but its revenue
 * figures use a different definition (not PAID-only) and should not be treated as reconciling with
 * the newer dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;
    private final EnquiryRepository enquiryRepository;
    private final LeadRepository    leadRepository;

    @IsStaffOrAdmin
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        ZoneId nairobi    = ZoneId.of("Africa/Nairobi");
        Instant now       = Instant.now();
        ZonedDateTime zdt = ZonedDateTime.now(nairobi);

        Instant dayStart   = zdt.toLocalDate().atStartOfDay(nairobi).toInstant();
        Instant dayEnd     = zdt.toLocalDate().plusDays(1).atStartOfDay(nairobi).toInstant();
        Instant weekStart  = zdt.toLocalDate().minusDays(6).atStartOfDay(nairobi).toInstant();
        Instant monthStart = zdt.toLocalDate().withDayOfMonth(1).atStartOfDay(nairobi).toInstant();

        BigDecimal revenueToday = orderRepository.sumPaidOrdersInRange(dayStart, dayEnd);
        BigDecimal revenueWeek  = orderRepository.sumPaidOrdersInRange(weekStart, now);
        BigDecimal revenueMTD   = orderRepository.sumPaidOrdersInRange(monthStart, now);

        long ordersToday   = orderRepository.countByCreatedAtBetween(dayStart, dayEnd);
        long ordersPending = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long ordersInProd  = orderRepository.countByStatus(OrderStatus.IN_PRODUCTION);
        long ordersTotal   = orderRepository.count();

        long totalProducts = productRepository.countActive();
        long totalUsers    = userRepository.findByDeletedFalse(PageRequest.of(0, 1)).getTotalElements();
        long totalEnquiries = enquiryRepository.count();
        long totalLeads     = leadRepository.count();

        List<String> topProducts = orderRepository
                .findTopSellingProducts(monthStart, PageRequest.of(0, 5))
                .stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("revenueToday",   revenueToday  != null ? revenueToday  : BigDecimal.ZERO);
        response.put("revenueWeek",    revenueWeek   != null ? revenueWeek   : BigDecimal.ZERO);
        response.put("revenueMTD",     revenueMTD    != null ? revenueMTD    : BigDecimal.ZERO);
        response.put("ordersToday",    ordersToday);
        response.put("ordersPending",  ordersPending);
        response.put("ordersInProd",   ordersInProd);
        response.put("ordersTotal",    ordersTotal);
        response.put("totalProducts",  totalProducts);
        response.put("totalUsers",     totalUsers);
        response.put("totalEnquiries", totalEnquiries);
        response.put("totalLeads",     totalLeads);
        response.put("topProducts",    topProducts);

        return ResponseEntity.ok(response);
    }
}