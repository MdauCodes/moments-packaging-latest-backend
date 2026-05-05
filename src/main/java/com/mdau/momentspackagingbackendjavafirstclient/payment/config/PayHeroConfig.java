package com.mdau.momentspackagingbackendjavafirstclient.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.payhero")
@Data
public class PayHeroConfig {
    private String apiUsername;
    private String apiPassword;
    private String channelId;
    private String baseUrl;
    private String callbackUrl;
}