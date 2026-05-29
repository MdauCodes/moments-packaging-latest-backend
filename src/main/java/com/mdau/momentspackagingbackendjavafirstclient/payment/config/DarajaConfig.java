package com.mdau.momentspackagingbackendjavafirstclient.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.daraja")
@Data
public class DarajaConfig {
    private String consumerKey;
    private String consumerSecret;
    private String shortcode;
    private String passkey;
    private String callbackUrl;
    private String baseUrl;
    /** CUSTOMER_PAYBILL_ONLINE or CUSTOMER_BUYGOODS_ONLINE */
    private String transactionType;
    private String accountReference;
    private String transactionDesc;
}