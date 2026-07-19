package com.mdau.momentspackagingbackendjavafirstclient.receipt.entity;

public enum ReceiptStatus {
    /** Row created, PDF not yet generated/uploaded. */
    PENDING,
    /** Generation/upload in progress — transient, so a crash mid-generation is visible rather than silently stuck at PENDING. */
    GENERATING,
    /** PDF generated, uploaded to Cloudinary, and emailed successfully. */
    SENT,
    /** Generation, upload, or email send failed — see failureReason. Retryable. */
    FAILED,
}
