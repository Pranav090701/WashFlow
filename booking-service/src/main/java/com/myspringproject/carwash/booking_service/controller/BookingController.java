package com.myspringproject.carwash.booking_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.booking_service.service.BookingService;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{bookingId}/validate")
    public ResponseEntity<Boolean> validateOrder(@PathVariable UUID bookingId,
                                                @RequestParam UUID customerId) {
        boolean valid = bookingService.isValidBooking(bookingId, customerId);
        return ResponseEntity.ok(valid);
    }

    /**
     * Mark a booking as completed.
     *
     * @param bookingId UUID of the booking to mark as completed
     * @return Success message if booking is marked completed
     */
    @PatchMapping("/{bookingId}/complete")
    public ResponseEntity<String> markBookingCompleted(@PathVariable UUID bookingId) {
        bookingService.markBookingCompleted(bookingId);
        return ResponseEntity.ok("Booking marked as completed.");
    }
}
