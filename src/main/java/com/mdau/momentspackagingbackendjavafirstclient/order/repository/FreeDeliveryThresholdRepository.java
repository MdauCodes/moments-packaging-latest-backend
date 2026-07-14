package com.mdau.momentspackagingbackendjavafirstclient.order.repository;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FreeDeliveryThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FreeDeliveryThresholdRepository extends JpaRepository<FreeDeliveryThreshold, UUID> {
    List<FreeDeliveryThreshold> findByActiveTrueOrderByMinOrderAmountAsc();
}
