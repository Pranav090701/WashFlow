package com.myspringproject.carwash.booking_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.myspringproject.carwash.booking_service.client.WasherClientService;
import com.myspringproject.carwash.booking_service.entity.Slot;
import com.myspringproject.carwash.booking_service.entity.Slot.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SlotInitializerScheduler {

    private final SlotService slotService;
    private final WasherClientService washerClientService;
    private final SlotAvailabilityPolicy slotAvailabilityPolicy;

    private static final Logger logger = LoggerFactory.getLogger(SlotInitializerScheduler.class);

    public SlotInitializerScheduler(SlotService slotService,
            WasherClientService washerClientService,
            SlotAvailabilityPolicy slotAvailabilityPolicy) {
        this.slotService = slotService;
        this.washerClientService = washerClientService;
        this.slotAvailabilityPolicy = slotAvailabilityPolicy;
    }

    /**
     * This scheduled method runs every day at 11:58 PM.
     * It keeps a rolling booking window cached for today, tomorrow, and the day
     * after tomorrow.
     */
    @Scheduled(cron = "0 58 23 * * *")
    public void createAndCacheSlotsForBookingWindow() {
        slotService.removePastAvailableSlotKeys();

        LocalDate today = LocalDate.now();
        List<UUID> availableWashers = washerClientService.getAvailableWasherIds();

        if (availableWashers.isEmpty()) {
            logger.error("No available washers found while preparing booking window");
            return;
        }

        for (int dayOffset = 0; dayOffset <= 2; dayOffset++) {
            LocalDate slotDate = today.plusDays(dayOffset);
            for (UUID washerId : availableWashers) {
                List<Slot> slots = createHourlySlots(washerId, slotDate);
                slotService.saveSlotsAndCache(slots);
            }
        }

        logger.info("Initialized and cached rolling booking window for {} washers", availableWashers.size());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createAndCacheSlotsOnStartup() {
        createAndCacheSlotsForBookingWindow();
    }

    /**
     * Generates 1-hour slots for a washer from 6 AM to 6 PM.
     */
    private List<Slot> createHourlySlots(UUID washerId, LocalDate date) {
        List<Slot> slots = new ArrayList<>();
        LocalTime start = slotAvailabilityPolicy.workStart();
        LocalTime end = slotAvailabilityPolicy.workEnd();

        while (!start.isAfter(end.minusHours(1))) {
            Slot slot = Slot.builder()
                    .washerId(washerId)
                    .date(date)
                    .startTime(start)
                    .endTime(start.plusHours(1))
                    .status(SlotStatus.AVAILABLE)
                    .build();
            slots.add(slot);
            start = start.plusHours(1);
        }

        return slots;
    }
}
