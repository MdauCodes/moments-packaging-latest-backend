package com.mdau.momentspackagingbackendjavafirstclient.auth.service;

import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.CustomerProfileDto;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.CustomerProfileUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CustomerProfileDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new CustomerProfileDto(user);
    }

    @Transactional
    public CustomerProfileDto updateProfile(UUID userId, CustomerProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName()       != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()        != null) user.setLastName(request.getLastName());
        if (request.getPhone()           != null) user.setPhone(request.getPhone());
        if (request.getDeliveryAddress() != null) user.setDeliveryAddress(request.getDeliveryAddress());
        if (request.getCity()            != null) user.setCity(request.getCity());
        if (request.getCounty()          != null) user.setCounty(request.getCounty());
        if (request.getPostalCode()      != null) user.setPostalCode(request.getPostalCode());
        if (request.getBusinessName()    != null) user.setBusinessName(request.getBusinessName());

        return new CustomerProfileDto(userRepository.save(user));
    }
}