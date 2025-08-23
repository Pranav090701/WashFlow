package com.myspringproject.carwash.customer_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.customer_service.dto.CustomerProfileDTO;
import com.myspringproject.carwash.customer_service.entity.CustomerProfile;
import com.myspringproject.carwash.customer_service.service.CustomerProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/customerProfile")
public class CustomerProfileController {

    private final CustomerProfileService profileService;

    public CustomerProfileController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Create a new customer profile.
     * 
     * @param userId  UUID of the user (from "X-User-Id" header)
     * @param request CustomerProfileDTO object with profile details
     * @return The created CustomerProfile
     *
     */
    @PostMapping
    public ResponseEntity<CustomerProfile> createCustomerProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody CustomerProfileDTO request) {
        CustomerProfile created = profileService.createCustomerProfile(userId, request);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Get a customer profile by user ID.
     * 
     * @param id UUID of the user (as path variable)
     * @return The CustomerProfile for the given user ID
     *
     */
    @GetMapping("/{id}")
    public CustomerProfile getCustomerProfile(@PathVariable UUID id) {
        return profileService.getCustomerProfile(id);
    }

    /**
     * Update the customer profile for the given user.
     * 
     * @param id      UUID of the user (from "X-User-Id" header)
     * @param profile CustomerProfileDTO object with updated details
     * @return The updated CustomerProfile
     *
     */
    @PutMapping
    public CustomerProfile updateCustomerProfile(@RequestHeader("X-User-Id") UUID id, @RequestBody CustomerProfileDTO profile) {
        return profileService.updateCustomerProfile(id, profile);
    }
}