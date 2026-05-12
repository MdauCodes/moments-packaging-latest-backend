package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.DeliveryZone;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.DeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryZoneService {

    private final DeliveryZoneRepository deliveryZoneRepository;

    @Transactional(readOnly = true)
    public BigDecimal getFeeForCounty(String county) {
        return deliveryZoneRepository.findByCountyIgnoreCase(county)
                .filter(DeliveryZone::getActive)
                .map(DeliveryZone::getFeeAmount)
                .orElse(BigDecimal.ZERO);
    }

    /** Public — active zones only for checkout dropdown */
    @Transactional(readOnly = true)
    public List<DeliveryZone> getAllActive() {
        return deliveryZoneRepository.findAllByActiveTrue();
    }

    /** Admin — all zones including inactive */
    @Transactional(readOnly = true)
    public List<DeliveryZone> getAll() {
        return deliveryZoneRepository.findAllByOrderByZoneNameAsc();
    }

    @Transactional
    public DeliveryZone create(String zoneName, String county,
                                BigDecimal fee, String description) {
        DeliveryZone zone = DeliveryZone.builder()
                .zoneName(zoneName)
                .county(county)
                .feeAmount(fee)
                .description(description)
                .active(true)
                .build();
        return deliveryZoneRepository.save(zone);
    }

    @Transactional
    public DeliveryZone update(UUID id, String zoneName, String county,
                                BigDecimal fee, Boolean active, String description) {
        DeliveryZone zone = deliveryZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery zone not found"));
        if (zoneName    != null) zone.setZoneName(zoneName);
        if (county      != null) zone.setCounty(county);
        if (fee         != null) zone.setFeeAmount(fee);
        if (active      != null) zone.setActive(active);
        if (description != null) zone.setDescription(description);
        return deliveryZoneRepository.save(zone);
    }

    @Transactional
    public void delete(UUID id) {
        deliveryZoneRepository.deleteById(id);
    }
}