package com.mdau.momentspackagingbackendjavafirstclient.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublicConfigDto {

    private Boolean blogsEnabled;
    private Boolean emailCaptureEnabled;
    private String  whatsappNumber;
    private String  companyEmail;
    private String  companyPhone;

    /** Printed on customer invoices — empty string until an admin sets it in Settings. */
    private String  businessKraPin;
}