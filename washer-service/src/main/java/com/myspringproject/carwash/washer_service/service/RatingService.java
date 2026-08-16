package com.myspringproject.carwash.washer_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.washer_service.client.BookingClient;
import com.myspringproject.carwash.washer_service.dto.RatingDTO;
import com.myspringproject.carwash.washer_service.entity.Rating;
import com.myspringproject.carwash.washer_service.repository.RatingRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final WasherProfileService washerService;
    private final BookingClient bookingClient;
    private final String bookingValidationToken;

    public RatingService(RatingRepository ratingRepository, WasherProfileService washerService,
            BookingClient bookingClient,
            @Value("${carwash.internal.booking-validation-token}") String bookingValidationToken) {
        this.ratingRepository = ratingRepository;
        this.washerService = washerService;
        this.bookingClient = bookingClient;
        this.bookingValidationToken = bookingValidationToken;
    }

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);

    /**
     * Fetches all ratings for a given washer.
     *
     * @param washerId UUID of the washer
     * @return List of Rating entities for the washer
     */
    public List<Rating> getRatingsForWasher(UUID washerId) {
        logger.info("Fetching ratings for washer {}", washerId);

        List<Rating> ratings = ratingRepository.findByWasherId(washerId);

        logger.info("Completed Fetching ratings for washer {}", washerId);
        logger.info("Ratings - {}", ratings);
        return ratings;
    }

    /**
     * Calculates the average rating score for a given washer.
     *
     * @param washerId UUID of the washer
     * @return Average rating as Double (0.0 if no ratings)
     */
    public Double calculateAverageRating(UUID washerId) {
        List<Rating> ratings = getRatingsForWasher(washerId);

        return ratings.stream()
                .mapToInt(Rating::getRatingScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Adds a new rating for a washer by a customer.
     * Validates the booking before allowing the rating.
     * Uses circuit breaker and retry for booking service communication.
     *
     * @param washerId   UUID of the washer being rated
     * @param customerId UUID of the customer giving the rating
     * @param request    RatingDTO containing rating score, review, and bookingId
     * @return The saved Rating entity
     * @throws IllegalArgumentException if booking is invalid or missing
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @CircuitBreaker(name = "bookingService") //, fallbackMethod = "bookingServiceFallback"
    @Retry(name = "bookingService")
    public Rating addRating(UUID washerId, UUID customerId, RatingDTO request) {
        logger.info("Adding rating {} for washer {} ", request, washerId);
        UUID bookingId = request.getBookingId();

        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID is required");
        }

        Boolean isValid = bookingClient.validateBooking(bookingId, customerId, bookingValidationToken);
        if (!Boolean.TRUE.equals(isValid)) {
            throw new IllegalArgumentException("Invalid booking for customer");
        }

        Rating rating = new Rating();
        rating.setWasherId(washerId);
        rating.setBookingId(bookingId);
        rating.setCustomerId(customerId);
        rating.setRatingScore(request.getRatingScore());
        rating.setReview(request.getReview());
        rating.setTimestamp(Instant.now());

        Rating savedRating = ratingRepository.save(rating);

        washerService.updateAverageRating(washerId, rating.getRatingScore());

        logger.info("succesfully added rating {}", savedRating);
        return savedRating;
    }

    /**
     * Fallback method for addRating when booking service is unavailable or retries fail.
     *
     * @param washerId   UUID of the washer
     * @param customerId UUID of the customer
     * @param request    RatingDTO with rating details
     * @param t          Throwable cause of the fallback
     * @throws BookingServiceNotAvailableException always thrown to indicate service is down
     */
    /*@SuppressWarnings("java:S1172") //falsePositive
    public Rating bookingServiceFallback(UUID washerId, UUID customerId, RatingDTO request,
            Throwable t) {
        logger.error("Booking validation failed for booking {}: {}", request.getBookingId(), t.getMessage());
        throw new BookingServiceNotAvailableException("Booking Service is temporarily unavailable. Please try again later.",t.getCause());
    }*/
}
