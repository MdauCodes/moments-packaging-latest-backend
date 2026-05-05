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
}