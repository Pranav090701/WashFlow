package com.myspringproject.carwash.washer_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.washer_service.dto.RatingDTO;
import com.myspringproject.carwash.washer_service.entity.Rating;
import com.myspringproject.carwash.washer_service.service.RatingService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/washer")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{id}/ratings")
    public List<Rating> getRatings(@PathVariable UUID id) {
        return ratingService.getRatingsForWasher(id);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{id}/averageRatings")
    public Double getAverageRatings(@PathVariable UUID id) {
        return ratingService.calculateAverageRating(id);
    }

    @PostMapping("/{washerId}/ratings")
    public ResponseEntity<Rating> addRating(@PathVariable UUID washerId,
                                            @RequestBody RatingDTO request,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Rating rating = ratingService.addRating(washerId,customerId,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }
}