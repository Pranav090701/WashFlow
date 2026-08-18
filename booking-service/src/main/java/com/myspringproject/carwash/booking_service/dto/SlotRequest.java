package com.myspringproject.carwash.booking_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * This DTO captures the slot booking request from the user.
 * It includes the washer's ID, date of the wash, and time range.
 */
public class SlotRequest {

    @NotNull(message = "Washer ID is required")
    private UUID washerId;

    @NotNull(message = "Date is required (yyyy-MM-dd format)")
    private LocalDate date;

    @NotNull(message = "Start time is required (HH:mm format)")
    private LocalTime startTime;

    public SlotRequest() {}

    public SlotRequest(UUID washerId, LocalDate date, LocalTime startTime) {
        this.washerId = washerId;
        this.date = date;
        this.startTime = startTime;
    }

    public UUID getWasherId() {
        return washerId;
    }

    public void setWasherId(UUID washerId) {
        this.washerId = washerId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
}

