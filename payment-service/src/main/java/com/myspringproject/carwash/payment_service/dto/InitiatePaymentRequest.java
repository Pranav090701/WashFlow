package com.myspringproject.carwash.payment_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public class InitiatePaymentRequest {

    @NotNull
    private UUID washerId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime slotTime;

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

    public LocalTime getSlotTime() {
        return slotTime;
    }

    public void setSlotTime(LocalTime slotTime) {
        this.slotTime = slotTime;
    }

}
