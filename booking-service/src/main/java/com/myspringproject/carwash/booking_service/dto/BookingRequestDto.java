package com.myspringproject.carwash.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for booking confirmation request.
 * Customer/userId is NOT included here, it will come from the request header injected by API Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {

    @NotNull(message = "Washer ID is required")
    private UUID washerId;

    @NotNull(message = "Slot time is required (HH:mm format)")
    private LocalTime slotTime;

    @NotNull(message = "Date is required (yyyy-MM-dd format)")
    private LocalDate date;

    @NotNull(message = "Price is required")
    private Double price;
}
