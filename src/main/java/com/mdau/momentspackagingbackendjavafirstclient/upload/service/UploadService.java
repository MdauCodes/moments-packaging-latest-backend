package com.mdau.momentspackagingbackendjavafirstclient.upload.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private static final int    MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final byte[] JPEG_MAGIC     = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC      = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_MAGIC     = {0x52, 0x49, 0x46, 0x46};

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.upload-folder}")
    private String uploadFolder;

    /**
     * Uploads a non-image file (e.g. a generated PDF) as a Cloudinary "raw" resource — the
     * image-only validate/transform pipeline in uploadImage() doesn't apply here. publicId is
     * returned alongside the URL since deleting a raw asset later (see the tax-document cleanup
     * job) requires it, not just the URL.
     */
    public UploadResponse uploadRaw(byte[] bytes, String entity, String filename) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("File is empty");
        }
        if (bytes.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        try {
            String folder = uploadFolder + "/" + entity;
            Map<?, ?> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder",        folder,
                            "resource_type", "raw",
                            "public_id",     filename
                    )
            );
            String url      = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            log.info("Uploaded raw file to Cloudinary: {}", publicId);
            return new UploadResponse(url, publicId);
        } catch (IOException e) {
            log.error("Cloudinary raw upload failed: {}", e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    /**
     * Signs a direct-to-Cloudinary upload for a raw file the frontend will render and upload
     * itself (see TaxDocumentService) — the API secret never leaves the backend, only a
     * short-lived signature scoped to this exact folder/public_id/timestamp does.
     */
    public UploadSignature signRawUpload(String entity, String filename) {
        long timestamp = Instant.now().getEpochSecond();
        String folder = uploadFolder + "/" + entity;
        Map<String, Object> paramsToSign = ObjectUtils.asMap(
                "timestamp",  timestamp,
                "folder",     folder,
                "public_id",  filename
        );
        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);
        return new UploadSignature(cloudinary.config.cloudName, cloudinary.config.apiKey, signature, timestamp, folder, filename);
    }

    /** Deletes a previously uploaded raw resource (see the weekly tax-document cleanup job). */
    public void deleteRaw(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.info("Deleted raw file from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Cloudinary raw delete failed for {}: {}", publicId, e.getMessage());
            throw new RuntimeException("File delete failed: " + e.getMessage());
        }
    }

    public UploadResponse uploadImage(MultipartFile file, String entity) {
        validateFile(file);

        try {
            String folder = uploadFolder + "/" + entity;
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",          folder,
                            "resource_type",   "image",
                            "quality",         "auto",
                            "fetch_format",    "auto"
                    )
            );

            String url      = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            log.info("Uploaded image to Cloudinary: {}", publicId);
            return new UploadResponse(url, publicId);

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new IllegalArgumentException("File too small to validate");
            }

            if (!isJpeg(header) && !isPng(header) && !isWebp(header)) {
                throw new IllegalArgumentException(
                        "Invalid file type. Only JPEG, PNG and WebP are allowed.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read file: " + e.getMessage());
        }
    }

    private boolean isJpeg(byte[] h) {
        return h[0] == JPEG_MAGIC[0] && h[1] == JPEG_MAGIC[1] && h[2] == JPEG_MAGIC[2];
    }

    private boolean isPng(byte[] h) {
        return h[0] == PNG_MAGIC[0] && h[1] == PNG_MAGIC[1]
            && h[2] == PNG_MAGIC[2] && h[3] == PNG_MAGIC[3];
    }

    private boolean isWebp(byte[] h) {
        // RIFF....WEBP
        return h[0] == WEBP_MAGIC[0] && h[1] == WEBP_MAGIC[1]
            && h[2] == WEBP_MAGIC[2] && h[3] == WEBP_MAGIC[3];
    }
}