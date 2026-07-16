package com.mdau.momentspackagingbackendjavafirstclient.upload.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Everything the frontend needs to upload a raw file directly to Cloudinary without the API secret ever leaving the backend. */
@Getter
@AllArgsConstructor
public class UploadSignature {
    private String cloudName;
    private String apiKey;
    private String signature;
    private long   timestamp;
    private String folder;
    private String publicId;
}
