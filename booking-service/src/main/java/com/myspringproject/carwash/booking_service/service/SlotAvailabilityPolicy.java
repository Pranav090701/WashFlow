package com.myspringproject.carwash.booking_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

@Component
public class SlotAvailabilityPolicy {

    private static final LocalTime WORK_START = LocalTime.of(6, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);
    private static final int BOOKING_WINDOW_DAYS_AFTER_TODAY = 2;
    private static final int SAME_DAY_MINIMUM_LEAD_HOURS = 1;

    public LocalDate latestBookableDate() {
        return LocalDate.now().plusDays(BOOKING_WINDOW_DAYS_AFTER_TODAY);
    }

    public LocalTime workStart() {
        return WORK_START;
    }

    public LocalTime workEnd() {
        return WORK_END;
    }

    public boolean isBookable(LocalDate date, LocalTime startTime) {
        if (date == null || startTime == null) {
            return false;
        }

        if (!isInBookingWindow(date) || !isWorkingHourStart(startTime)) {
            return false;
        }
        LocalDateTime minimumStartTime = LocalDateTime.now().plusHours(SAME_DAY_MINIMUM_LEAD_HOURS);
        return !date.atTime(startTime).isBefore(minimumStartTime);
    }

    public void validateBookableDate(LocalDate date) {
        if (!isInBookingWindow(date)) {
            throw new IllegalArgumentException("Slots can be booked only for today, tomorrow, or the day after tomorrow.");
        }
    }

    public void validateLockableSlot(LocalDate date, LocalTime startTime) {
        validateBookableDate(date);
        if (!isBookable(date, startTime)) {
            throw new IllegalArgumentException("Slot must be within working hours and at least 1 hour from now.");
        }
    }

    public void validateLockedSlotCanProceed(LocalDate date, LocalTime startTime) {
        validateBookableDate(date);
        if (!isWorkingHourStart(startTime) || date.atTime(startTime).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Slot must be within working hours and must not be in the past.");
        }
    }

    private boolean isInBookingWindow(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date != null && !date.isBefore(today) && !date.isAfter(latestBookableDate());
    }

    private boolean isWorkingHourStart(LocalTime startTime) {
        return startTime != null && !startTime.isBefore(WORK_START) && startTime.isBefore(WORK_END);
    }
}
