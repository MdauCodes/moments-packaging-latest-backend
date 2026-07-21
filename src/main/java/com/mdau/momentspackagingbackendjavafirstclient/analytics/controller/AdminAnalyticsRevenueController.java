package com.mdau.momentspackagingbackendjavafirstclient.analytics.controller;

import com.mdau.momentspackagingbackendjavafirstclient.analytics.dto.RevenueSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.analytics.service.AnalyticsService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Comprehensive analytics dashboard, built phase by phase — Phase 1 is revenue/payment health.
 * Deliberately a new controller (not added to the existing AdminAnalyticsController, which stays
 * untouched at /overview) so the old fixed-window endpoint keeps working unmodified until the
 * final consolidation phase, once the new dashboard fully replaces it.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsRevenueController {

    private final AnalyticsService analyticsService;

    @IsStaffOrAdmin
    @GetMapping("/revenue")
    public RevenueSummaryDto revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return analyticsService.getRevenueSummary(from, to);
    }
}
