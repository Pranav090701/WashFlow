package com.myspringproject.carwash.booking_service.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.booking_service.dto.AdminBookingResponse;
import com.myspringproject.carwash.booking_service.entity.Booking.BookingStatus;
import com.myspringproject.carwash.booking_service.service.BookingService;

@RestController
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookingsForAdmin());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<AdminBookingResponse> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBookingForAdmin(bookingId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AdminBookingResponse>> getBookingsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(bookingService.getBookingsForCustomerForAdmin(customerId));
    }

    @GetMapping("/washer/{washerId}")
    public ResponseEntity<List<AdminBookingResponse>> getBookingsByWasher(@PathVariable UUID washerId) {
        return ResponseEntity.ok(bookingService.getBookingsForWasherForAdmin(washerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AdminBookingResponse>> getBookingsByStatus(@PathVariable BookingStatus status) {
        return ResponseEntity.ok(bookingService.getBookingsByStatusForAdmin(status));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AdminBookingResponse>> getBookingsByDate(@PathVariable LocalDate date) {
        return ResponseEntity.ok(bookingService.getBookingsByDateForAdmin(date));
    }
}
