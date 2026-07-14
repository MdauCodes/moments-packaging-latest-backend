package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FreeDeliveryThreshold;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.FreeDeliveryThresholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class FreeDeliveryThresholdService {

    private final FreeDeliveryThresholdRepository repository;

    @Transactional(readOnly = true)
    public List<FreeDeliveryThreshold> getAllActive() {
        return repository.findByActiveTrueOrderByMinOrderAmountAsc();
    }

    @Transactional(readOnly = true)
    public List<FreeDeliveryThreshold> getAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(FreeDeliveryThreshold::getMinOrderAmount))
                .toList();
    }

    @Transactional
    public FreeDeliveryThreshold create(BigDecimal minOrderAmount, String zoneLabel, Integer sortOrder) {
        return repository.save(FreeDeliveryThreshold.builder()
                .minOrderAmount(minOrderAmount)
                .zoneLabel(zoneLabel)
                .active(true)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public FreeDeliveryThreshold update(UUID id, BigDecimal minOrderAmount, String zoneLabel,
                                         Boolean active, Integer sortOrder) {
        FreeDeliveryThreshold t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threshold not found: " + id));
        if (minOrderAmount != null) t.setMinOrderAmount(minOrderAmount);
        if (zoneLabel != null) t.setZoneLabel(zoneLabel);
        if (active != null) t.setActive(active);
        if (sortOrder != null) t.setSortOrder(sortOrder);
        return repository.save(t);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Threshold not found: " + id);
        repository.deleteById(id);
    }
}
