package com.mdau.momentspackagingbackendjavafirstclient.user.entity;

/**
 * Fine-grained permissions that drive both backend access control
 * and frontend UI gating. JWT carries the full permissions list so
 * the frontend never needs a separate API call to determine what a
 * user can see or do.
 */
public enum Permission {

    // ── Order permissions ─────────────────────────────────────────
    ORDER_VIEW,              // see orders (all roles that touch orders)
    ORDER_VERIFY_PAYMENT,    // PAID → PAYMENT_VERIFIED
    ORDER_PREPARE,           // PAYMENT_VERIFIED → IN_PRODUCTION → READY_FOR_DISPATCH
    ORDER_DISPATCH,          // READY_FOR_DISPATCH → DISPATCHED + item checklist
    ORDER_ASSIGN,            // assign orders to staff (supervisor)
    ORDER_MANAGE_ALL,        // override any status, refund, cancel

    // ── Product permissions ───────────────────────────────────────
    PRODUCT_VIEW,
    PRODUCT_MANAGE,          // create / edit / delete

    // ── Payment permissions ───────────────────────────────────────
    PAYMENT_VIEW,

    // ── User / role management ────────────────────────────────────
    USER_VIEW,
    USER_CREATE,             // create staff accounts
    USER_MANAGE_ROLES,       // assign roles / permissions (SUPER_ADMIN only)

    // ── Analytics & settings ──────────────────────────────────────
    ANALYTICS_VIEW,
    SETTINGS_MANAGE
}