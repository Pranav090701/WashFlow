package com.myspringproject.carwash.customer_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.customer_service.dto.CustomerProfileDTO;
import com.myspringproject.carwash.customer_service.entity.CustomerProfile;
import com.myspringproject.carwash.customer_service.exception.ProfileAlreadyExistsException;
import com.myspringproject.carwash.customer_service.exception.ProfileNotFoundException;
import com.myspringproject.carwash.customer_service.repository.CustomerProfileRepository;

import java.util.UUID;

@Service
public class CustomerProfileService {

    private final CustomerProfileRepository profileRepository;

    public CustomerProfileService(CustomerProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(CustomerProfileService.class);

    /**
     * Creates a new customer profile for the given user ID.
     * Throws ProfileAlreadyExistsException if a profile already exists for the user.
     *
     * @param userId      UUID of the user for whom the profile is being created
     * @param profileData CustomerProfileDTO object containing profile details
     * @return The created CustomerProfile
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerProfile createCustomerProfile(UUID userId, CustomerProfileDTO profileData) {
        logger.info("Creating customer profile for userId: {}", userId);
        if (profileRepository.existsById(userId)) {
            throw new ProfileAlreadyExistsException("CustomerProfile already exists for userId " + userId);
        }

        CustomerProfile profile = new CustomerProfile(userId,profileData.getFullName(),
                profileData.getPhoneNumber(), profileData.getAddress(), profileData.getProfilePictureUrl());
        
        return profileRepository.save(profile);
    }

    /**
     * Retrieves the customer profile for the given user ID.
     * Throws ProfileNotFoundException if no profile exists for the user.
     *
     * @param userId UUID of the user whose profile is to be retrieved
     * @return The CustomerProfile for the given user ID
     */
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'WASHER')")
    public CustomerProfile getCustomerProfile(UUID userId) {
        logger.info("Retrieving customer profile for userId: {}", userId);
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("CustomerProfile not found for user ID: " + userId));
    }

     /**
     * Updates the customer profile for the given user ID.
     * Throws ProfileNotFoundException if no profile exists for the user.
     *
     * @param userId                 UUID of the user whose profile is to be updated
     * @param updatedCustomerProfile CustomerProfile object containing updated details
     * @return The updated CustomerProfile
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerProfile updateCustomerProfile(UUID userId, CustomerProfileDTO updatedCustomerProfile) {
        logger.info("Updating customer profile for userId: {}", userId);
        CustomerProfile existing = profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("CustomerProfile not found for user ID: " + userId));

        existing.setFullName(updatedCustomerProfile.getFullName());
        existing.setPhoneNumber(updatedCustomerProfile.getPhoneNumber());
        existing.setAddress(updatedCustomerProfile.getAddress());
        existing.setProfilePictureUrl(updatedCustomerProfile.getProfilePictureUrl());

        return profileRepository.save(existing);
    }
}
