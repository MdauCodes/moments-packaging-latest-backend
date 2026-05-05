package com.mdau.momentspackagingbackendjavafirstclient.settings.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_settings", indexes = {
        @Index(name = "idx_app_settings_key", columnList = "setting_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(length = 255)
    private String description;
}