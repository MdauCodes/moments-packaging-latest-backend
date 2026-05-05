package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnquiryCreateRequest {

    private String persona;

    @NotNull(message = "Contact details are required")
    @Valid
    private ContactRequest contact;

    private String message;
    private String source;

    @Getter
    @Setter
    public static class ContactRequest {
        @NotBlank(message = "Contact name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        private String phone;
        private String company;
    }
}