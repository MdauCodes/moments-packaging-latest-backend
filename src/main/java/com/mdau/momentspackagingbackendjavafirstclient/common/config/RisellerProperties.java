package com.mdau.momentspackagingbackendjavafirstclient.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.riseller")
public class RisellerProperties {

    private String xAppKey;
    private String stockUrl;
    private int pageSize = 100;
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String serviceUrl;
        private String bucketName;
        private String accessKey;
        private String secretKey;
        private String folderName;
        private String fileName;
    }
}