package com.myspringproject.carwash.booking_service.controller;

import com.myspringproject.carwash.booking_service.dto.BookingRequestDto;
import com.myspringproject.carwash.booking_service.dto.SlotRequest;
import com.myspringproject.carwash.booking_service.entity.Booking;
import com.myspringproject.carwash.booking_service.service.SlotInitializerScheduler;
import com.myspringproject.carwash.booking_service.service.SlotService;

import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for handling slot-related APIs.
 */
@RestController
@RequestMapping("/slots")
public class SlotController {

    private final SlotService slotService;
    private final SlotInitializerScheduler slotInitializerScheduler;

    public SlotController(SlotService slotService,
                          SlotInitializerScheduler slotInitializerScheduler) {
        this.slotService = slotService;
        this.slotInitializerScheduler = slotInitializerScheduler;
    }

    /**
     * API to get all available slots for a washer on a specific date.
    */
    @GetMapping("/available")
    public ResponseEntity<Set<String>> getAvailableSlotsTimingsForWasher(
            @RequestParam UUID washerId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(slotService.getAvailableSlotsTimingsForWasher(washerId, date));
    }

    /**
     * API to lock a slot for 10 minutes.
    */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/lock")
    public ResponseEntity<String> lockSlot(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @Valid @RequestBody SlotRequest slotRequest) {
        slotService.lockSlot(slotRequest, customerId);
        return ResponseEntity.ok("Slot locked for 10 minutes.");
    }

    /**
     * API to confirm the slot booking after successful payment.
    */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/confirm")
    public ResponseEntity<Booking> confirmSlot(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @Valid @RequestBody BookingRequestDto bookingRequest) {
        Booking booking = slotService.confirmSlot(
                bookingRequest, customerId);
        return ResponseEntity.ok(booking);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<String> generateSlotsForWasher(
            ) {
        slotInitializerScheduler.createAndCacheSlotsForNextDay();
        return ResponseEntity.ok("Slots generated for washers for date "+ LocalDate.now().plusDays(1));
    }
}
