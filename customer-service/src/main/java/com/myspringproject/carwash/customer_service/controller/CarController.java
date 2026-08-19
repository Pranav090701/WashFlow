package com.myspringproject.carwash.customer_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.customer_service.dto.CarDTO;
import com.myspringproject.carwash.customer_service.entity.Car;
import com.myspringproject.carwash.customer_service.service.CarService;

@RequestMapping("/cars")
@RestController
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    /**
     * Add a new car for the user.
     *
     * @param requestCarObject Car object containing car details (brand, model, color, year, plateNumber)
     * @param userId           UUID of the user (from "X-User-Id" header)
     * @return The created Car entity
     *
     */
    @PostMapping
    public ResponseEntity<Car> addCar(@RequestBody CarDTO requestCarObject,@RequestHeader("X-User-Id") UUID userId) {
        Car car = carService.addCar(requestCarObject,userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(car);
    }

    /**
     * Get all cars for the user.
     *
     * @param userId UUID of the user (from "X-User-Id" header)
     * @return List of Car entities for the user
     *
     */
    @GetMapping
    public ResponseEntity<List<Car>> getCarsByUserId(@RequestHeader("X-User-Id") UUID userId) {
        List<Car> cars = carService.getCarsByUserId(userId);
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/customer/{userId}")
    public ResponseEntity<List<Car>> getCarsForCustomer(@PathVariable UUID userId) {
        List<Car> cars = carService.getCarsByUserId(userId);
        return ResponseEntity.ok(cars);
    }

    /**
     * Update an existing car for the user.
     *
     * @param requestCarObject Car object with updated details (must include ID)
     * @param userId           UUID of the user (from "X-User-Id" header)
     * @param carId            UUID of the car to update (as path variable)
     * @return The updated Car entity
     *
     */
    @PutMapping("/update/{carId}")
    public ResponseEntity<Car> updateCar(@RequestBody CarDTO requestCarObject,@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID carId) {
        Car car = carService.updateCar(requestCarObject,userId, carId);
        return ResponseEntity.ok(car);
    }

    /**
     * Delete a car by its ID for the user.
     *
     * @param id     UUID of the car to delete (as path variable)
     * @param userId UUID of the user (from "X-User-Id" header)
     * @return Success message
     *
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCar(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        carService.deleteCarById(id,userId);
        return ResponseEntity.ok("Car deleted successfully.");
    }

}
