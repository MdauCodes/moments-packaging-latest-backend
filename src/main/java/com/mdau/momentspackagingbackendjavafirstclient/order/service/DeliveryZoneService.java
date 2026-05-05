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

    @Transactional(readOnly = true)
    public List<DeliveryZone> getAllActive() {
        return deliveryZoneRepository.findAllByActiveTrue();
    }

    @Transactional
    public DeliveryZone create(String county, BigDecimal fee) {
        DeliveryZone zone = DeliveryZone.builder()
                .county(county).feeAmount(fee).active(true).build();
        return deliveryZoneRepository.save(zone);
    }

    @Transactional
    public DeliveryZone update(UUID id, String county, BigDecimal fee, Boolean active) {
        DeliveryZone zone = deliveryZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery zone not found"));
        if (county != null) zone.setCounty(county);
        if (fee    != null) zone.setFeeAmount(fee);
        if (active != null) zone.setActive(active);
        return deliveryZoneRepository.save(zone);
    }

    @Transactional
    public void delete(UUID id) {
        deliveryZoneRepository.deleteById(id);
    }
}