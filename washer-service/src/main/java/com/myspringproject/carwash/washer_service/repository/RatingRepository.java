package com.myspringproject.carwash.washer_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myspringproject.carwash.washer_service.entity.Rating;

import java.util.List;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    List<Rating> findByWasherId(UUID washerId);
}