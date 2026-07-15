package com.mdau.momentspackagingbackendjavafirstclient.customer.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.security.JwtService;
import com.mdau.momentspackagingbackendjavafirstclient.customer.dto.CustomerDto;
import com.mdau.momentspackagingbackendjavafirstclient.customer.service.AdminCustomerService;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private static final long IMPERSONATION_EXPIRATION_MS = 15 * 60 * 1000L;

    private final AdminCustomerService customerService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<CustomerDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String segment,
            @PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(customerService.list(q, status, segment, pageable));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        CustomerDto customer = customerService.getById(id);
        PageResponse<OrderDto> orders = orderService.getAllOrders(
                null, id, PageRequest.of(0, 50, Sort.by("createdAt").descending()));

        Map<String, Object> response = new HashMap<>();
        response.put("customer", customer);
        response.put("orders", orders.getContent());
        return ResponseEntity.ok(response);
    }

    /**
     * Mints a short-lived (15 min), no-refresh-token session for the target customer so admin
     * staff can open and act inside their real dashboard for support purposes. Restricted to
     * full admins (not general staff) since this is full impersonation, not a read-only view —
     * every mutating action taken under this token is still traceable back to the admin via the
     * IMPERSONATION audit entry below plus the impersonatedBy JWT claim.
     */
    @IsAdmin
    @PostMapping("/{id}/impersonate")
    public ResponseEntity<Map<String, Object>> impersonate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin,
            HttpServletRequest httpRequest) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        if (Boolean.TRUE.equals(target.getIsStaff())) {
            throw new ConflictException("Cannot impersonate a staff or admin account.");
        }

        String token = jwtService.generateImpersonationToken(target, admin.getId(), IMPERSONATION_EXPIRATION_MS);
        auditLogService.log(admin, "IMPERSONATION", id.toString(), target.getFullName(), "START",
                null, null, AuditLogService.extractIp(httpRequest));

        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", token);
        body.put("expiresIn", IMPERSONATION_EXPIRATION_MS / 1000);
        body.put("customerName", target.getFullName());
        body.put("accountType", target.getAccountType() != null ? target.getAccountType().name() : null);
        return ResponseEntity.ok(body);
    }
}
