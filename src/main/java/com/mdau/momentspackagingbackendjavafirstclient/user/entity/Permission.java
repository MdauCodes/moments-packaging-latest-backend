package com.mdau.momentspackagingbackendjavafirstclient.user.entity;

public enum Permission {

    // Order permissions
    ORDER_VIEW,
    ORDER_VERIFY_PAYMENT,
    ORDER_PREPARE,
    ORDER_DISPATCH,
    ORDER_ASSIGN,
    ORDER_MANAGE_ALL,

    // Product permissions
    PRODUCT_VIEW,
    PRODUCT_MANAGE,

    // Payment permissions
    PAYMENT_VIEW,
    PAYMENT_REFUND,

    // User / role management
    USER_VIEW,
    USER_CREATE,
    USER_MANAGE_ROLES,

    // Analytics & settings
    ANALYTICS_VIEW,
    SETTINGS_MANAGE,

    // Audit log
    AUDIT_VIEW,

    // Enquiry / CRM
    ENQUIRY_MANAGE,
    ENQUIRY_VIEW,

    // Content
    BLOG_MANAGE,
    REVIEW_MODERATE,
    CUSTOMER_VIEW
}