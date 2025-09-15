package com.myspringproject.carwash.customer_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.myspringproject.carwash.customer_service.dto.CustomerProfileDTO;
import com.myspringproject.carwash.customer_service.entity.CustomerProfile;
import com.myspringproject.carwash.customer_service.exception.ProfileAlreadyExistsException;
import com.myspringproject.carwash.customer_service.exception.ProfileNotFoundException;
import com.myspringproject.carwash.customer_service.repository.CustomerProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.test.context.support.WithMockUser;

class CustomerProfileServiceUnitTest {

    @Mock
    private CustomerProfileRepository profileRepository;

    @InjectMocks
    private CustomerProfileService customerProfileService;

    private UUID userId;
    private CustomerProfileDTO profileDTO;
    private CustomerProfile profile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        profileDTO = new CustomerProfileDTO("John Doe", "1234567890", "picUrl");
        profile = new CustomerProfile(userId, "John Doe", "1234567890", "picUrl");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createCustomerProfile_success() {
        when(profileRepository.existsById(userId)).thenReturn(false);
        when(profileRepository.save(any(CustomerProfile.class))).thenReturn(profile);

        CustomerProfile result = customerProfileService.createCustomerProfile(userId, profileDTO);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(profileRepository).save(any(CustomerProfile.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createCustomerProfile_alreadyExists_throwsException() {
        when(profileRepository.existsById(userId)).thenReturn(true);

        assertThrows(ProfileAlreadyExistsException.class, () -> {
            customerProfileService.createCustomerProfile(userId, profileDTO);
        });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomerProfile_success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        CustomerProfile result = customerProfileService.getCustomerProfile(userId);

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomerProfile_notFound_throwsException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> {
            customerProfileService.getCustomerProfile(userId);
        });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCustomerProfile_success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(CustomerProfile.class))).thenReturn(profile);

        CustomerProfileDTO updatedDTO = new CustomerProfileDTO("Jane Doe", "9876543210", "newPicUrl");
        CustomerProfile updated = customerProfileService.updateCustomerProfile(userId, updatedDTO);

        assertEquals("Jane Doe", updated.getFullName());
        assertEquals("9876543210", updated.getPhoneNumber());
        assertEquals("newPicUrl", updated.getProfilePictureUrl());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCustomerProfile_notFound_throwsException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        CustomerProfileDTO updatedDTO = new CustomerProfileDTO("Jane Doe", "9876543210", "newPicUrl");
        assertThrows(ProfileNotFoundException.class, () -> {
            customerProfileService.updateCustomerProfile(userId, updatedDTO);
        });
    }
}
