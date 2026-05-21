package com.mdau.momentspackagingbackendjavafirstclient.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("industries",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .build());

        manager.registerCustomCache("public-config",
                Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(10)
                        .build());

        manager.registerCustomCache("recommended-products",
                Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(20)
                        .build());

        manager.registerCustomCache("product-search",
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build());

        /**
         * Idempotency cache for checkout.
         * Key: idempotencyKey from client (UUID).
         * Value: order reference string.
         * TTL: 5 minutes — wide enough to absorb double-taps and slow connections.
         */
        manager.registerCustomCache("checkout-idempotency",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(5000)
                        .build());

        /**
         * Idempotency cache for payment initiation.
         * Key: orderId string.
         * Value: "PROCESSING" or "SUCCESS" — blocks duplicate STK pushes.
         * TTL: 2 minutes — enough for M-Pesa STK to resolve.
         */
        manager.registerCustomCache("payment-idempotency",
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(2000)
                        .build());

        return manager;
    }
}