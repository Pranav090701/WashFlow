package com.myspringproject.carwash.booking_service.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.booking_service.exception.UnauthorizedAccessException;
import com.myspringproject.carwash.booking_service.service.BookingService;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    
    private final BookingService bookingService;
    private final String bookingValidationToken;

    public BookingController(BookingService bookingService,
                             @Value("${carwash.internal.booking-validation-token}") String bookingValidationToken) {
        this.bookingService = bookingService;
        this.bookingValidationToken = bookingValidationToken;
    }

    @GetMapping("/{bookingId}/validate")
    public ResponseEntity<Boolean> validateOrder(@PathVariable UUID bookingId,
                                                 @RequestParam UUID customerId,
                                                 @RequestHeader("X-Internal-Service-Token") String internalToken) {
        if (!bookingValidationToken.equals(internalToken)) {
            throw new UnauthorizedAccessException("Invalid internal service token for booking validation");
        }

        boolean valid = bookingService.isValidBooking(bookingId, customerId);
        return ResponseEntity.ok(valid);
    }

    /**
     * Mark a booking as completed.
     *
     * @param bookingId UUID of the booking to mark as completed
     * @return Success message if booking is marked completed
     */
    @PreAuthorize("hasAnyRole('WASHER', 'ADMIN')")
    @PatchMapping("/{bookingId}/complete")
    public ResponseEntity<String> markBookingCompleted(@PathVariable UUID bookingId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        boolean adminRequest = "ADMIN".equalsIgnoreCase(role);
        UUID washerId = adminRequest ? null : UUID.fromString(jwt.getSubject());

        bookingService.markBookingCompleted(bookingId, washerId, adminRequest);
        return ResponseEntity.ok("Booking marked as completed.");
    }
}
