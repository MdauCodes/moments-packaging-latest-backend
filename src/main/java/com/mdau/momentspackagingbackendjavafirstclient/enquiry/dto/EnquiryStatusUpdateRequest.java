package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnquiryStatusUpdateRequest {

    private EnquiryStatus status;
    private String        assignedTo;
    private String        internalNotes;
}