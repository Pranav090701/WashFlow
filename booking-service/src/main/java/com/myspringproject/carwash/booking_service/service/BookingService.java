package com.myspringproject.carwash.booking_service.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.booking_service.dto.AdminBookingResponse;
import com.myspringproject.carwash.booking_service.entity.Booking;
import com.myspringproject.carwash.booking_service.entity.Booking.BookingStatus;
import com.myspringproject.carwash.booking_service.entity.Slot;
import com.myspringproject.carwash.booking_service.exception.BookingNotFoundException;
import com.myspringproject.carwash.booking_service.exception.SlotNotFoundException;
import com.myspringproject.carwash.booking_service.exception.UnauthorizedAccessException;
import com.myspringproject.carwash.booking_service.repository.BookingRepository;
import com.myspringproject.carwash.booking_service.repository.SlotRepository;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;

    public BookingService(BookingRepository bookingRepository, SlotRepository slotRepository) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
    }

    /**
     * Validate if a booking belongs to a customer
     */
    public boolean isValidBooking(UUID bookingId, UUID customerId) {
        logger.info("Validating booking {} for customer {}", bookingId, customerId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        if(!booking.getUserId().equals(customerId)){
            throw new UnauthorizedAccessException("Booking " +  bookingId + " does not belong to the customer "+ customerId);
        }
        return  booking.getStatus() == BookingStatus.COMPLETED;
    }

    /**
     * Mark a booking as completed.
     *
     * @param bookingId    UUID of the booking to mark as completed
     * @param washerId     UUID of the washer making the request; not required for admin
     * @param adminRequest true when the request is made by an admin
     * @throws BookingNotFoundException if booking does not exist
     */
    public void markBookingCompleted(UUID bookingId, UUID washerId, boolean adminRequest) {
        logger.info("Marking booking {} as completed", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        if (!adminRequest) {
            Slot slot = slotRepository.findById(booking.getSlotId())
                    .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + booking.getSlotId()));

            if (!slot.getWasherId().equals(washerId)) {
                throw new UnauthorizedAccessException(
                        "Booking " + bookingId + " does not belong to washer " + washerId);
            }
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        logger.info("Booking {} marked as completed", bookingId);
    }

    public List<AdminBookingResponse> getAllBookingsForAdmin() {
        return bookingRepository.findAll().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public AdminBookingResponse getBookingForAdmin(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        return toAdminResponse(booking);
    }

    public List<AdminBookingResponse> getBookingsForCustomerForAdmin(UUID customerId) {
        return bookingRepository.findByUserId(customerId).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public List<AdminBookingResponse> getBookingsForCustomer(UUID customerId) {
        return bookingRepository.findByUserId(customerId).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public List<AdminBookingResponse> getBookingsForWasherForAdmin(UUID washerId) {
        List<UUID> slotIds = slotRepository.findByWasherId(washerId).stream()
                .map(Slot::getId)
                .toList();
        return getBookingsBySlotIds(slotIds);
    }

    public List<AdminBookingResponse> getBookingsByStatusForAdmin(BookingStatus status) {
        return bookingRepository.findByStatus(status).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public List<AdminBookingResponse> getBookingsByDateForAdmin(LocalDate date) {
        List<UUID> slotIds = slotRepository.findByDate(date).stream()
                .map(Slot::getId)
                .toList();
        return getBookingsBySlotIds(slotIds);
    }

    private List<AdminBookingResponse> getBookingsBySlotIds(List<UUID> slotIds) {
        if (slotIds.isEmpty()) {
            return List.of();
        }

        return bookingRepository.findBySlotIdIn(slotIds).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    private AdminBookingResponse toAdminResponse(Booking booking) {
        Slot slot = slotRepository.findById(booking.getSlotId())
                .orElse(null);

        return new AdminBookingResponse(
                booking.getId(),
                booking.getSlotId(),
                booking.getUserId(),
                slot != null ? slot.getWasherId() : null,
                slot != null ? slot.getDate() : null,
                slot != null ? slot.getStartTime() : null,
                slot != null ? slot.getEndTime() : null,
                booking.getStatus(),
                booking.getPrice(),
                booking.getPaymentTime(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }

}
