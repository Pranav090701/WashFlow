package com.myspringproject.carwash.washer_service.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "booking-service", url = "${booking.service.base-url}", path = "/bookings")
public interface BookingClient {

    @GetMapping("/{bookingId}/validate")
    Boolean validateBooking(@PathVariable("bookingId") UUID bookingId,
                            @RequestParam("customerId") UUID customerId);
}