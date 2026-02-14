package com.myspringproject.carwash.booking_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(SlotInitializerScheduler.class);

    public SlotInitializerScheduler(SlotService slotService,
            WasherClientService washerClientService) {
        this.slotService = slotService;
        this.washerClientService = washerClientService;
    }

    /**
     * This scheduled method runs every day at 11:58 PM
     * It initializes the next day's slots for each washer and caches them as
     * available.
     */
    @Scheduled(cron = "0 58 23 * * *")
    public void createAndCacheSlotsForNextDay() {
        LocalDate nextDay = LocalDate.now().plusDays(1);
        List<UUID> availableWashers = washerClientService.getAvailableWasherIds();

        if (availableWashers.isEmpty()) {
            logger.error("No available washers found for {}", nextDay);
            return;
        }

        for (UUID washerId : availableWashers) {
            List<Slot> slots = createHourlySlots(washerId, nextDay);
            slotService.saveSlotsAndCache(slots);
        }

        logger.info("Initialized and cached slots for {} washers for {}", availableWashers.size(), nextDay);
    }

    /**
     * Generates 1-hour slots for a washer from 6 AM to 6 PM.
     */
    private List<Slot> createHourlySlots(UUID washerId, LocalDate date) {
        List<Slot> slots = new ArrayList<>();
        LocalTime start = LocalTime.of(6, 0);
        LocalTime end = LocalTime.of(18, 0);

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
