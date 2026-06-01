package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class EnquiryUpdateRequest {

    /** New pipeline status. Null = no change. */
    private EnquiryStatus status;

    /** Staff user UUID to assign. Null = no change. */
    private UUID assignedToId;

    /** Name label for assignee (snapshot). */
    private String assignedToName;

    /**
     * Note to append to internalNotes.
     * Each note is timestamped and appended, old notes preserved.
     */
    private String note;

    /** Scheduled follow-up datetime. */
    private Instant followUpAt;

    /** Estimated deal value in KES. */
    private BigDecimal estimatedValue;

    /** Products the customer is interested in. */
    private String productInterest;
}