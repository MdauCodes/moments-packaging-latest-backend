package com.mdau.momentspackagingbackendjavafirstclient.settings.controller;

import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.BlogStatus;
import com.mdau.momentspackagingbackendjavafirstclient.blog.repository.BlogRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.lead.repository.LeadRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.DashboardStatsDto;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ProductRepository productRepository;
    private final BlogRepository    blogRepository;
    private final EnquiryRepository enquiryRepository;
    private final LeadRepository    leadRepository;
    private final OrderRepository   orderRepository;

    @IsStaffOrAdmin
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        ZoneId nairobi = ZoneId.of("Africa/Nairobi");
        ZonedDateTime todayStart = ZonedDateTime.now(nairobi).toLocalDate()
                .atStartOfDay(nairobi);
        ZonedDateTime todayEnd   = todayStart.plusDays(1);
        ZonedDateTime monthStart = ZonedDateTime.now(nairobi).toLocalDate()
                .withDayOfMonth(1).atStartOfDay(nairobi);

        long totalProducts    = productRepository.count();
        long totalBlogs       = blogRepository.count();
        long totalEnquiries   = enquiryRepository.count();
        long totalLeads       = leadRepository.count();
        long unpublishedBlogs = blogRepository
                .findAllByStatusAndTemplate(BlogStatus.DRAFT, null, PageRequest.of(0, 1))
                .size();

        long ordersToday      = orderRepository.countByCreatedAtBetween(
                todayStart.toInstant(), todayEnd.toInstant());
        long ordersPending    = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long ordersInProd     = orderRepository.countByStatus(OrderStatus.IN_PRODUCTION);

        BigDecimal revenueToday = orderRepository.sumPaidOrdersInRange(
                todayStart.toInstant(), todayEnd.toInstant());
        BigDecimal revenueMTD   = orderRepository.sumPaidOrdersInRange(
                monthStart.toInstant(), Instant.now());

        List<String> topProducts = orderRepository
                .findTopSellingProducts(monthStart.toInstant(), PageRequest.of(0, 5))
                .stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());

        return ResponseEntity.ok(new DashboardStatsDto(
                totalProducts, totalBlogs, totalEnquiries, 0L,
                totalLeads, unpublishedBlogs,
                ordersToday, ordersPending, ordersInProd,
                revenueToday, revenueMTD, topProducts));
    }
}