package com.mdau.momentspackagingbackendjavafirstclient.upload.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.ApiError;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @IsStaffOrAdmin
    @PostMapping(value = "/image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String entity) {
        try {
            UploadResponse response = uploadService.uploadImage(file, entity);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiError error = ApiError.of(
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    "Unprocessable Entity",
                    "INVALID_FILE",
                    e.getMessage(),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 16)
            );
            return ResponseEntity.unprocessableEntity().body(error);
        } catch (RuntimeException e) {
            ApiError error = ApiError.of(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "UPLOAD_FAILED",
                    e.getMessage(),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 16)
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}