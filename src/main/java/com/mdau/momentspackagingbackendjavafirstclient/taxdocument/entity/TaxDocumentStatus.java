package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity;

public enum TaxDocumentStatus {
    /** Requested (at checkout or by admin), PDF not yet generated/uploaded. */
    PENDING,
    /** Generation/upload in progress — transient, exists mainly so a crash mid-generation is visible rather than silently stuck at PENDING. */
    GENERATING,
    /** PDF generated, uploaded to Cloudinary, and emailed successfully. */
    SENT,
    /** Generation, upload, or email send failed — see failureReason. Retryable. */
    FAILED,
    /** The weekly cleanup job deleted the Cloudinary asset after the 2-week retention window. Not an error — working as intended. */
    EXPIRED,
}
