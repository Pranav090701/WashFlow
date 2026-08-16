package com.myspringproject.carwash.washer_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.myspringproject.carwash.washer_service.dto.WasherProfileDTO;
import com.myspringproject.carwash.washer_service.entity.WasherProfile;
import com.myspringproject.carwash.washer_service.service.WasherProfileService;

@RestController
@RequestMapping("/washer")
public class WasherProfileController {

    private final WasherProfileService profileService;

    public WasherProfileController(WasherProfileService profileService) {
        this.profileService = profileService;
    }

    // Create new profile (userId comes from JWT via gateway)
    @PreAuthorize("hasAnyRole('WASHER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<WasherProfile> createWasherProfile(
            @RequestHeader("X-Washer-Id") UUID userId,
            @RequestBody WasherProfileDTO request) {
        WasherProfile created = profileService.createWasherProfile(userId, request);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public WasherProfile getWasherProfile(@PathVariable UUID id) {
        return profileService.getWasherProfile(id);
    }

    @PreAuthorize("hasRole('WASHER')")
    @PutMapping
    public WasherProfile updateWasherProfile(@RequestHeader("X-Washer-Id") UUID userId,
            @RequestBody WasherProfileDTO profile) {
        return profileService.updateWasherProfile(userId, profile);
    }

    @PreAuthorize("hasAnyRole('WASHER', 'ADMIN')")
    @PatchMapping("/availability")
    public String updateAvailabilityStatus(@RequestHeader("X-Washer-Id") UUID userId,
                                           @RequestParam(value = "availability", required = false) Boolean availability,
                                           @RequestParam(value = "availablity", required = false) Boolean legacyAvailability) {
        Boolean requestedAvailability = availability != null ? availability : legacyAvailability;
        if (requestedAvailability == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "availability query parameter is required");
        }

        return profileService.updateAvailabilityStatus(userId, requestedAvailability);
    }

    // Fetch all washers in a given area and pincode
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/by-area")
    public ResponseEntity<List<WasherProfile>> getWashersByArea(
            @RequestParam String serviceArea,
            @RequestParam String pincode) {
        List<WasherProfile> washers = profileService.findWashersByAreaAndPincode(serviceArea, pincode);
        return ResponseEntity.ok(washers);
    }


    //Get id pf all washers with availability status true

    @GetMapping("/available")
    public ResponseEntity<List<UUID>> getAvailableWashers() {
        List<UUID> availableWashers = profileService.getAvailableWashersId();
        return ResponseEntity.ok(availableWashers);
    }
}
