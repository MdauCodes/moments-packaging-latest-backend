package com.mdau.momentspackagingbackendjavafirstclient.order.repository;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {
    Optional<DeliveryZone> findByCountyIgnoreCase(String county);
    List<DeliveryZone> findAllByActiveTrue();
    List<DeliveryZone> findAllByOrderByZoneNameAsc();
}