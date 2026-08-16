package com.myspringproject.carwash.booking_service.controller;

import com.myspringproject.carwash.booking_service.dto.BookingRequestDto;
import com.myspringproject.carwash.booking_service.dto.LockedSlotQuoteResponse;
import com.myspringproject.carwash.booking_service.dto.SlotRequest;
import com.myspringproject.carwash.booking_service.entity.Booking;
import com.myspringproject.carwash.booking_service.exception.UnauthorizedAccessException;
import com.myspringproject.carwash.booking_service.service.SlotInitializerScheduler;
import com.myspringproject.carwash.booking_service.service.SlotService;

import org.springframework.beans.factory.annotation.Value;
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
    private final String bookingConfirmationToken;

    public SlotController(SlotService slotService,
                          SlotInitializerScheduler slotInitializerScheduler,
                          @Value("${carwash.internal.booking-confirmation-token}") String bookingConfirmationToken) {
        this.slotService = slotService;
        this.slotInitializerScheduler = slotInitializerScheduler;
        this.bookingConfirmationToken = bookingConfirmationToken;
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
     * Internal API to price a locked slot before payment order creation.
    */
    @PostMapping("/locked-quote")
    public ResponseEntity<LockedSlotQuoteResponse> getLockedSlotQuote(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @Valid @RequestBody BookingRequestDto bookingRequest) {
        if (!bookingConfirmationToken.equals(internalToken)) {
            throw new UnauthorizedAccessException("Invalid internal service token for locked slot quote");
        }

        return ResponseEntity.ok(slotService.getLockedSlotQuote(bookingRequest, customerId));
    }

    /**
     * Internal API to confirm the slot booking after successful payment.
    */
    @PostMapping("/confirm")
    public ResponseEntity<Booking> confirmSlot(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @Valid @RequestBody BookingRequestDto bookingRequest) {
        if (!bookingConfirmationToken.equals(internalToken)) {
            throw new UnauthorizedAccessException("Invalid internal service token for booking confirmation");
        }

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
