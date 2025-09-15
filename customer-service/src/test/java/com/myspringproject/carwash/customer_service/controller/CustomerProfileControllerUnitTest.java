package com.myspringproject.carwash.customer_service.controller;

import com.myspringproject.carwash.customer_service.config.SecurityConfig;
import com.myspringproject.carwash.customer_service.dto.CustomerProfileDTO;
import com.myspringproject.carwash.customer_service.entity.CustomerProfile;
import com.myspringproject.carwash.customer_service.service.CustomerProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerProfileController.class)
@Import(SecurityConfig.class)
class CustomerProfileControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean // <-- Use this for now
    private CustomerProfileService profileService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createCustomerProfile_success() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomerProfile profile = new CustomerProfile(userId, "John Doe", "1234567890", "picUrl");

        Mockito.when(profileService.createCustomerProfile(eq(userId), any(CustomerProfileDTO.class))).thenReturn(profile);

        mockMvc.perform(post("/customerProfile")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fullName": "John Doe",
                        "phoneNumber": "1234567890",
                        "profilePictureUrl": "picUrl"
                    }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomerProfile_success() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomerProfile profile = new CustomerProfile(userId, "John Doe", "1234567890", "picUrl");

        Mockito.when(profileService.getCustomerProfile(userId)).thenReturn(profile);

        mockMvc.perform(get("/customerProfile/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCustomerProfile_success() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomerProfile updatedProfile = new CustomerProfile(userId, "Jane Doe", "9876543210", "newPicUrl");

        Mockito.when(profileService.updateCustomerProfile(eq(userId), any(CustomerProfileDTO.class))).thenReturn(updatedProfile);

        mockMvc.perform(put("/customerProfile")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fullName": "Jane Doe",
                        "phoneNumber": "9876543210",
                        "profilePictureUrl": "newPicUrl"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"));
    }
}
