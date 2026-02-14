package com.myspringproject.carwash.booking_service.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.booking_service.cache.SlotCacheService;
import com.myspringproject.carwash.booking_service.dto.BookingRequestDto;
import com.myspringproject.carwash.booking_service.dto.SlotRequest;
import com.myspringproject.carwash.booking_service.entity.Booking;
import com.myspringproject.carwash.booking_service.entity.Booking.BookingStatus;
import com.myspringproject.carwash.booking_service.entity.Slot;
import com.myspringproject.carwash.booking_service.entity.Slot.SlotStatus;
import com.myspringproject.carwash.booking_service.exception.SlotLockExpiredException;
import com.myspringproject.carwash.booking_service.exception.SlotNotFoundException;
import com.myspringproject.carwash.booking_service.exception.UnauthorizedAccessException;
import com.myspringproject.carwash.booking_service.repository.BookingRepository;
import com.myspringproject.carwash.booking_service.repository.SlotRepository;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SlotService {

    private SlotRepository slotRepository;

    private SlotCacheService slotCacheService;

    private BookingRepository bookingRepository;

    /**
     * Constructor for SlotService
     * 
     * @param slotRepository    Repository for Slot entity
     * @param slotCacheService  Service for caching slot data
     * @param bookingRepository Repository for Booking entity
     */
    public SlotService(SlotRepository slotRepository, SlotCacheService slotCacheService,
            BookingRepository bookingRepository) {
        this.slotRepository = slotRepository;
        this.slotCacheService = slotCacheService;
        this.bookingRepository = bookingRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(SlotService.class);

    /**
     * Get available slots for a washer from cache
     */
    public Set<String> getAvailableSlotsTimingsForWasher(UUID washerId, LocalDate date) {
        return slotCacheService.getAvailableSlotsTiming(washerId.toString(), date.toString());
    }

    /**
     * Lock a slot for 10 minutes.
     * 
     * @param slotRequest DTO containing washerId, date, startTime
     * @param customerId  UUID of the customer locking the slot (from API Gateway
     *                    header)
     */
    public void lockSlot(SlotRequest slotRequest, UUID customerId) {
        boolean locked = slotCacheService.lockSlot(
                slotRequest.getWasherId().toString(),
                slotRequest.getDate().toString(),
                slotRequest.getStartTime().toString(),
                customerId);

        if (!locked) {
            throw new SlotNotFoundException("Slot is already locked or not available.");
        }
    }

    /**
     * Book a slot after locking and availability checks
     */
    @Transactional
    public Booking confirmSlot(BookingRequestDto bookingDTO, UUID customerId) {
        String time = bookingDTO.getSlotTime().toString();
        UUID washerId = bookingDTO.getWasherId();
        LocalDate date = bookingDTO.getDate();

        UUID lockOwner = slotCacheService.getLockOwner(washerId.toString(), date.toString(), time);
        if (lockOwner == null) {
            throw new SlotLockExpiredException("Slot lock expired or not found.");
        }
        if (!lockOwner.equals(customerId)) {
            throw new UnauthorizedAccessException("This slot is locked by another customer.");
        }

        Slot slot = slotRepository.findByWasherIdAndDateAndStartTime(washerId, date, LocalTime.parse(time));
        if (slot == null) {
            throw new SlotNotFoundException(
                    "Slot not found in DB for washerId: " + washerId + ", date: " + date + ", time: " + time);
        }

        // 3. Save booking details in Booking table
        Booking booking = new Booking(
                slot.getId(),
                customerId,
                BookingStatus.CONFIRMED,
                bookingDTO.getPrice(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now());
        bookingRepository.save(booking);

        // 2. Update slot status in DB

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        // 4. Remove from lock cache
        slotCacheService.removeLock(washerId.toString(), date.toString(), time);

        // 5. Remove from available slots cache if still present
        slotCacheService.removeFromAvailableSlots(
                washerId.toString(),
                date.toString(),
                time);

        logger.info("Slot booked successfully for washerId: {}, date: {}, time: {}", washerId, date, time);
        return booking;
    }

    /**
     * Save slots and cache them as available
     */
    public void saveSlotsAndCache(List<Slot> slots) {
        slotRepository.saveAll(slots);
        for (Slot slot : slots) {
            slotCacheService.addAvailableSlot(slot.getWasherId().toString(), slot.getDate().toString(),
                    slot.getStartTime().toString());
        }
        logger.info("Saved and cached {} slots for washer {}", slots.size(), slots.get(0).getWasherId());
    }


}
