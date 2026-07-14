package com.mdau.momentspackagingbackendjavafirstclient.customer.service;

import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessType;
import com.mdau.momentspackagingbackendjavafirstclient.business.repository.BusinessAccountRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.customer.dto.CustomerDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditWallet;
import com.mdau.momentspackagingbackendjavafirstclient.referral.repository.CreditWalletRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.AccountType;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin-facing customer directory — segment and status are computed, not
 * stored: there is no separate "customer profile" table, so this derives a
 * readable record straight from the User + their paid order history each
 * request. Fine for the data volumes here; would need indexing/caching if
 * the customer base grows into the tens of thousands.
 */
@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private static final BigDecimal VIP_LIFETIME_THRESHOLD = BigDecimal.valueOf(100_000);
    private static final long ACTIVE_WITHIN_DAYS = 60;
    private static final long AT_RISK_WITHIN_DAYS = 180;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BusinessAccountRepository businessAccountRepository;
    private final CreditWalletRepository creditWalletRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> list(String q, String status, String segment, Pageable pageable) {
        List<User> matches = userRepository.searchCustomers(q == null ? "" : q.trim());

        List<CustomerDto> enriched = matches.stream()
                .map(this::toDto)
                .filter(c -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || c.getStatus().name().equalsIgnoreCase(status))
                .filter(c -> segment == null || segment.isBlank() || "ALL".equalsIgnoreCase(segment)
                        || c.getSegment().name().equalsIgnoreCase(segment))
                .sorted(Comparator.comparing(CustomerDto::getCreatedAt).reversed())
                .toList();

        int start = Math.min((int) pageable.getOffset(), enriched.size());
        int end = Math.min(start + pageable.getPageSize(), enriched.size());
        Page<CustomerDto> page = new PageImpl<>(enriched.subList(start, end), pageable, enriched.size());
        return new PageResponse<>(page);
    }

    @Transactional(readOnly = true)
    public CustomerDto getById(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsStaff()) && !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        return toDto(user);
    }

    private CustomerDto toDto(User user) {
        Object[] stats = orderRepository.getOrderSummaryForCustomer(user).get(0);
        long orderCount = (Long) stats[0];
        BigDecimal lifetimeValue = (BigDecimal) stats[1];
        Instant firstOrderAt = (Instant) stats[2];
        Instant lastOrderAt = (Instant) stats[3];

        BigDecimal avgOrderValue = orderCount > 0
                ? lifetimeValue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Integer rewardsPoints = user.getAccountType() == AccountType.INDIVIDUAL_SHOPPER
                ? creditWalletRepository.findByUser(user).map(CreditWallet::getBalance).orElse(0)
                : null;

        return new CustomerDto(
                user.getId(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getEmail(),
                user.getPhone(),
                user.getCity(),
                resolveSegment(user),
                resolveStatus(lifetimeValue, orderCount, lastOrderAt),
                lifetimeValue,
                orderCount,
                lastOrderAt,
                firstOrderAt,
                avgOrderValue,
                user.getDeliveryAddress(),
                user.getCreatedAt(),
                user.getAccountType(),
                rewardsPoints);
    }

    /** RETAIL by default; a Business Account bumps the customer into a B2B segment. */
    private CustomerDto.Segment resolveSegment(User user) {
        return businessAccountRepository.findByUserId(user.getId())
                .map(acc -> acc.getBusinessType() == BusinessType.LIMITED_COMPANY
                        ? CustomerDto.Segment.ENTERPRISE
                        : CustomerDto.Segment.WHOLESALE)
                .orElse(CustomerDto.Segment.RETAIL);
    }

    private CustomerDto.Status resolveStatus(BigDecimal lifetimeValue, long orderCount, Instant lastOrderAt) {
        if (lifetimeValue.compareTo(VIP_LIFETIME_THRESHOLD) >= 0) return CustomerDto.Status.VIP;
        if (orderCount == 0 || lastOrderAt == null) return CustomerDto.Status.DORMANT;
        long daysSinceLastOrder = ChronoUnit.DAYS.between(lastOrderAt, Instant.now());
        if (daysSinceLastOrder <= ACTIVE_WITHIN_DAYS) return CustomerDto.Status.ACTIVE;
        if (daysSinceLastOrder <= AT_RISK_WITHIN_DAYS) return CustomerDto.Status.AT_RISK;
        return CustomerDto.Status.DORMANT;
    }
}
