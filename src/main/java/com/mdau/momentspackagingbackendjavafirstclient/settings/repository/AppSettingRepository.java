package com.mdau.momentspackagingbackendjavafirstclient.settings.repository;

import com.mdau.momentspackagingbackendjavafirstclient.settings.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    Optional<AppSetting> findByKey(String key);

    boolean existsByKey(String key);
}