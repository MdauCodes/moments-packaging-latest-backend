package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity;

public enum DocumentBundleStatus {
    /** Payment succeeded and the customer opted in — waiting on an admin to upload the ETR scan. */
    PENDING,
    /** ETR uploaded and the receipt + tax invoice + ETR bundle was emailed successfully. */
    SENT,
    /** ETR upload succeeded but the email send failed, or a retry failed — see failureReason. Retryable. */
    FAILED,
}
