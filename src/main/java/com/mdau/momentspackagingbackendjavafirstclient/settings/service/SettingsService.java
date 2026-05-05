package com.mdau.momentspackagingbackendjavafirstclient.settings.service;

import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingDto;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.settings.entity.AppSetting;
import com.mdau.momentspackagingbackendjavafirstclient.settings.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AppSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public List<SettingDto> getAllSettings() {
        return settingRepository.findAll()
                .stream()
                .map(SettingDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public SettingDto upsertSetting(SettingUpdateRequest request) {
        AppSetting setting = settingRepository.findByKey(request.getKey())
                .orElseGet(() -> AppSetting.builder()
                        .key(request.getKey())
                        .build());

        setting.setValue(request.getValue());
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }

        return new SettingDto(settingRepository.save(setting));
    }

    public String getValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(AppSetting::getValue)
                .orElse(defaultValue);
    }
}