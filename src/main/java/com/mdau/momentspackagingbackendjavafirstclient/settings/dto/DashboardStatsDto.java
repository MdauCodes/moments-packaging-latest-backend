package com.mdau.momentspackagingbackendjavafirstclient.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardStatsDto {
    private long       totalProducts;
    private long       totalBlogs;
    private long       totalEnquiries;
    private long       newEnquiries;
    private long       totalLeads;
    private long       unpublishedBlogs;
    private long       ordersToday;
    private long       ordersPending;
    private long       ordersInProduction;
    private BigDecimal revenueToday;
    private BigDecimal revenueMTD;
    private List<String> topSellingProducts;
}