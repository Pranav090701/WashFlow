package com.myspringproject.carwash.booking_service.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.booking_service.cache.SlotCacheService;
import com.myspringproject.carwash.booking_service.dto.BookingRequestDto;
import com.myspringproject.carwash.booking_service.dto.LockedSlotQuoteResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SlotService {

    private SlotRepository slotRepository;

    private SlotCacheService slotCacheService;

    private BookingRepository bookingRepository;
    private final BigDecimal standardWashAmount;
    private final String pricingCurrency;
    private final SlotAvailabilityPolicy slotAvailabilityPolicy;

    /**
     * Constructor for SlotService
     * 
     * @param slotRepository    Repository for Slot entity
     * @param slotCacheService  Service for caching slot data
     * @param bookingRepository Repository for Booking entity
     */
    public SlotService(SlotRepository slotRepository, SlotCacheService slotCacheService,
            BookingRepository bookingRepository,
            @Value("${carwash.pricing.standard-wash-amount}") BigDecimal standardWashAmount,
            @Value("${carwash.pricing.currency:INR}") String pricingCurrency,
            SlotAvailabilityPolicy slotAvailabilityPolicy) {
        this.slotRepository = slotRepository;
        this.slotCacheService = slotCacheService;
        this.bookingRepository = bookingRepository;
        this.standardWashAmount = standardWashAmount;
        this.pricingCurrency = pricingCurrency;
        this.slotAvailabilityPolicy = slotAvailabilityPolicy;
    }

    private static final Logger logger = LoggerFactory.getLogger(SlotService.class);

    /**
     * Get available slots for a washer from cache
     */
    public Set<String> getAvailableSlotsTimingsForWasher(UUID washerId, LocalDate date) {
        slotAvailabilityPolicy.validateBookableDate(date);

        Set<String> cachedSlots = slotCacheService.getAvailableSlotsTiming(washerId.toString(), date.toString());
        Set<String> bookableSlots = new LinkedHashSet<>();
        if (cachedSlots == null) {
            return bookableSlots;
        }

        for (String cachedSlot : cachedSlots) {
            LocalTime startTime = LocalTime.parse(cachedSlot);
            if (slotAvailabilityPolicy.isBookable(date, startTime)) {
                bookableSlots.add(cachedSlot);
            } else {
                slotCacheService.removeFromAvailableSlots(washerId.toString(), date.toString(), cachedSlot);
            }
        }

        return bookableSlots;
    }

    /**
     * Lock a slot for 10 minutes.
     * 
     * @param slotRequest DTO containing washerId, date, startTime
     * @param customerId  UUID of the customer locking the slot (from API Gateway
     *                    header)
     */
    public void lockSlot(SlotRequest slotRequest, UUID customerId) {
        slotAvailabilityPolicy.validateLockableSlot(slotRequest.getDate(), slotRequest.getStartTime());

        Slot slot = getSlot(slotRequest.getWasherId(), slotRequest.getDate(), slotRequest.getStartTime());
        ensureSlotIsNotBooked(slot);

        boolean locked = slotCacheService.lockSlot(
                slotRequest.getWasherId().toString(),
                slotRequest.getDate().toString(),
                slotRequest.getStartTime().toString(),
                customerId);

        if (!locked) {
            throw new SlotNotFoundException("Slot is already locked or not available.");
        }
    }

    public void releaseSlotLock(UUID washerId, LocalDate date, LocalTime startTime, UUID customerId) {
        slotAvailabilityPolicy.validateLockedSlotCanProceed(date, startTime);

        Slot slot = getSlot(washerId, date, startTime);
        ensureSlotIsNotBooked(slot);

        boolean released = slotCacheService.removeLockIfOwned(
                washerId.toString(),
                date.toString(),
                startTime.toString(),
                customerId);

        if (!released) {
            validateLockOwner(washerId, date, startTime, customerId);
        }

        if (slotAvailabilityPolicy.isBookable(date, startTime)) {
            slotCacheService.addAvailableSlot(
                    washerId.toString(),
                    date.toString(),
                    startTime.toString());
        }
    }

    /**
     * Validate that a slot is currently locked by this customer and return the
     * server-owned payable amount.
     */
    public LockedSlotQuoteResponse getLockedSlotQuote(BookingRequestDto bookingDTO, UUID customerId) {
        slotAvailabilityPolicy.validateLockedSlotCanProceed(bookingDTO.getDate(), bookingDTO.getSlotTime());

        Slot slot = getSlot(bookingDTO.getWasherId(), bookingDTO.getDate(), bookingDTO.getSlotTime());

        bookingRepository.findBySlotIdAndUserId(slot.getId(), customerId)
                .ifPresent(existing -> {
                    throw new SlotNotFoundException("Slot is already booked.");
                });
        ensureSlotIsNotBooked(slot);

        validateLockOwner(bookingDTO.getWasherId(), bookingDTO.getDate(), bookingDTO.getSlotTime(), customerId);

        return new LockedSlotQuoteResponse(
                bookingDTO.getWasherId(),
                bookingDTO.getDate(),
                bookingDTO.getSlotTime(),
                standardWashAmount,
                pricingCurrency);
    }

    /**
     * Book a slot after locking and availability checks
     */
    @Transactional
    public Booking confirmSlot(BookingRequestDto bookingDTO, UUID customerId) {
        String time = bookingDTO.getSlotTime().toString();
        UUID washerId = bookingDTO.getWasherId();
        LocalDate date = bookingDTO.getDate();

        slotAvailabilityPolicy.validateLockedSlotCanProceed(date, bookingDTO.getSlotTime());

        Slot slot = getSlot(washerId, date, bookingDTO.getSlotTime());

        var existingBooking = bookingRepository.findBySlotIdAndUserId(slot.getId(), customerId);
        if (existingBooking.isPresent()) {
            return existingBooking.get();
        }
        ensureSlotIsNotBooked(slot);

        validateLockOwner(washerId, date, bookingDTO.getSlotTime(), customerId);

        // 3. Save booking details in Booking table
        Booking booking = new Booking(
                slot.getId(),
                customerId,
                BookingStatus.CONFIRMED,
                standardWashAmount.doubleValue(),
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

    private Slot getSlot(UUID washerId, LocalDate date, LocalTime slotTime) {
        Slot slot = slotRepository.findByWasherIdAndDateAndStartTime(washerId, date, slotTime);
        if (slot == null) {
            throw new SlotNotFoundException(
                    "Slot not found in DB for washerId: " + washerId + ", date: " + date + ", time: " + slotTime);
        }
        return slot;
    }

    private void ensureSlotIsNotBooked(Slot slot) {
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new SlotNotFoundException("Slot is already booked.");
        }
    }

    private void validateLockOwner(UUID washerId, LocalDate date, LocalTime slotTime, UUID customerId) {
        UUID lockOwner = slotCacheService.getLockOwner(
                washerId.toString(),
                date.toString(),
                slotTime.toString());
        if (lockOwner == null) {
            throw new SlotLockExpiredException("Slot lock expired or not found.");
        }
        if (!lockOwner.equals(customerId)) {
            throw new UnauthorizedAccessException("This slot is locked by another customer.");
        }
    }

    /**
     * Save slots and cache them as available
     */
    public void saveSlotsAndCache(List<Slot> slots) {
        for (Slot slot : slots) {
            Slot persistedSlot = slotRepository.findByWasherIdAndDateAndStartTime(
                    slot.getWasherId(),
                    slot.getDate(),
                    slot.getStartTime());

            if (persistedSlot == null) {
                persistedSlot = slotRepository.save(slot);
            }

            if (persistedSlot.getStatus() == SlotStatus.AVAILABLE
                    && slotAvailabilityPolicy.isBookable(persistedSlot.getDate(), persistedSlot.getStartTime())
                    && !slotCacheService.isSlotLocked(
                            persistedSlot.getWasherId().toString(),
                            persistedSlot.getDate().toString(),
                            persistedSlot.getStartTime().toString())) {
                slotCacheService.addAvailableSlot(
                        persistedSlot.getWasherId().toString(),
                        persistedSlot.getDate().toString(),
                        persistedSlot.getStartTime().toString());
            } else {
                slotCacheService.removeFromAvailableSlots(
                        persistedSlot.getWasherId().toString(),
                        persistedSlot.getDate().toString(),
                        persistedSlot.getStartTime().toString());
            }
        }
        logger.info("Saved and cached {} slots for washer {}", slots.size(), slots.get(0).getWasherId());
    }

    public void removePastAvailableSlotKeys() {
        slotCacheService.removeAvailableSlotsBefore(LocalDate.now());
    }

}
