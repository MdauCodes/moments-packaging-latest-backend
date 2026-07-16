package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteUploadRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String cloudinaryUrl;

    @NotBlank
    private String cloudinaryPublicId;
}
