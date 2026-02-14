package com.myspringproject.carwash.booking_service.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.booking_service.entity.Booking;
import com.myspringproject.carwash.booking_service.entity.Booking.BookingStatus;
import com.myspringproject.carwash.booking_service.exception.BookingNotFoundException;
import com.myspringproject.carwash.booking_service.exception.UnauthorizedAccessException;
import com.myspringproject.carwash.booking_service.repository.BookingRepository;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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
     * @param bookingId UUID of the booking to mark as completed
     * @throws BookingNotFoundException if booking does not exist
     */
    public void markBookingCompleted(UUID bookingId) {
        logger.info("Marking booking {} as completed", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        logger.info("Booking {} marked as completed", bookingId);
    }

}
