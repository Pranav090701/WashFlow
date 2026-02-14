package com.myspringproject.carwash.washer_service.dto;

import java.util.UUID;

public class RatingDTO {

    private UUID bookingId;
    private int ratingScore;
    private String review;

    public UUID getBookingId() {
        return bookingId;
    }
    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
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

    @Override
    public String toString() {
        return "RatingDTO [bookingId=" + bookingId + ", ratingScore=" + ratingScore + ", review=" + review + "]";
    }

    
}
