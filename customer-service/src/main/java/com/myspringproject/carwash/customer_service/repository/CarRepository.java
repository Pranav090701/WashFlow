package com.myspringproject.carwash.customer_service.repository;

import org.springframework.stereotype.Repository;

import com.myspringproject.carwash.customer_service.entity.Car;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID>{
     
    List<Car> findByUserId(UUID userId);
}
