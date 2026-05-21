package com.mdau.momentspackagingbackendjavafirstclient.user.repository;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, UUID> {

    Optional<StaffRole> findByNameAndDeletedFalse(String name);

    List<StaffRole> findByDeletedFalseOrderByIsDefaultDescNameAsc();

    boolean existsByNameAndDeletedFalse(String name);
}