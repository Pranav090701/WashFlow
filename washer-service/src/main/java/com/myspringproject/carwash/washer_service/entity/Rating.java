package com.myspringproject.carwash.washer_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "washer_id", nullable = false)
    private UUID washerId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "rating_score", nullable = false)
    private int ratingScore;

    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public Rating() {
    }

    public Rating(UUID id,UUID bookingId, UUID washerId, UUID customerId, int ratingScore, String review, Instant timestamp) {
        this.id = id;
        this.bookingId = bookingId;
        this.washerId = washerId;
        this.customerId = customerId;
        this.ratingScore = ratingScore;
        this.review = review;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getWasherId() {
        return washerId;
    }

    public void setWasherId(UUID washerId) {
        this.washerId = washerId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public int getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(int ratingScore) {
        this.ratingScore = ratingScore;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Rating [id=" + id + ", washerId=" + washerId + ", customerId=" + customerId + ", ratingScore="
                + ratingScore + ", review=" + review + ", timestamp=" + timestamp + "]";
    }

    
}