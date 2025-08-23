package com.myspringproject.carwash.customer_service.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.customer_service.dto.CarDTO;
import com.myspringproject.carwash.customer_service.entity.Car;
import com.myspringproject.carwash.customer_service.exception.CarNotFoundException;
import com.myspringproject.carwash.customer_service.exception.CarOwnershipMismatchException;
import com.myspringproject.carwash.customer_service.repository.CarRepository;

@PreAuthorize("hasRole('CUSTOMER')")
@Service
public class CarService {

    private final CarRepository carRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public CarService(CarRepository carRepository, RedisTemplate<String, Object> redisTemplate) {
        this.carRepository = carRepository;
        this.redisTemplate = redisTemplate;
    }

    private static final Logger logger = LoggerFactory.getLogger(CarService.class);

    private static final String CARS_CACHE_PREFIX = "cars:user:";

    /**
     * Adds a new car for the given user.
     *
     * @param inputCar CarDTO object containing car details (brand, model, color, year, plateNumber)
     * @param userId   UUID of the user adding the car
     * @return The saved Car entity
     */
    public Car addCar(CarDTO inputCar, UUID userId) {
        Car car = new Car();
        car.setUserId(userId);
        car.setBrand(inputCar.getBrand());
        car.setModel(inputCar.getModel());
        car.setColor(inputCar.getColor());
        car.setYear(inputCar.getYear());
        car.setPlateNumber(inputCar.getPlateNumber());

        Car savedCar = carRepository.save(car);
        logger.info("Car added {}", savedCar);

        // Invalidate user's car list cache
        redisTemplate.delete(CARS_CACHE_PREFIX + userId);
        return savedCar;
    }

    /**
     * Retrieves all cars for a given user.
     * Uses Redis cache for performance.
     *
     * @param userId UUID of the user
     * @return List of Car entities for the user
     */
    @SuppressWarnings("unchecked")
    public List<Car> getCarsByUserId(UUID userId) {
        logger.info("Entered getCarsByUserId for {}", userId);
        // Optional: use Redis cache
        String cacheKey = CARS_CACHE_PREFIX + userId;
        List<Car> cached = (List<Car>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Car> allCars = carRepository.findByUserId(userId);
        logger.info("List of cars for user-id {} : {}", userId, allCars);

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, allCars);
        return allCars;
    }

    /**
     * Deletes a car by its ID for the given user.
     * Throws CarNotFoundException if the car does not exist.
     * Throws CarOwnershipMismatchException if the car does not belong to the user.
     *
     * @param carId  UUID of the car to delete
     * @param userId UUID of the user requesting deletion
     */
    public void deleteCarById(UUID carId, UUID userId) {
        Car existingCar = carRepository.findById(carId)
                .orElseThrow(() -> new CarNotFoundException("Car with id" + carId + " not found"));
        carRepository.delete(existingCar);

        if(!existingCar.getUserId().equals(userId)){
            throw new CarOwnershipMismatchException(existingCar.getId(),userId);
        }

        // Invalidate user's car list cache
        redisTemplate.delete(CARS_CACHE_PREFIX + existingCar.getUserId());
    }

    /**
     * Updates an existing car for the given user.
     * Throws CarNotFoundException if the car does not exist.
     * Throws CarOwnershipMismatchException if the car does not belong to the user.
     *
     * @param car    Car object with updated details (must include ID)
     * @param userId UUID of the user updating the car
     * @param carId  UUID of the car to update
     * @return The updated Car entity
     */
    public Car updateCar(CarDTO car, UUID userId, UUID carId) {
        Car existingCar = carRepository.findById(carId)
                .orElseThrow(() -> new CarNotFoundException("Car with id" + carId + " not found"));

        if(!existingCar.getUserId().equals(userId)){
            throw new CarOwnershipMismatchException(carId,userId);
        }

        existingCar.setBrand(car.getBrand());
        existingCar.setColor(car.getColor());
        existingCar.setModel(car.getModel());
        existingCar.setPlateNumber(car.getPlateNumber());
        existingCar.setYear(car.getYear());

        carRepository.save(existingCar);

        // Invalidate user's car list cache
        redisTemplate.delete(CARS_CACHE_PREFIX + existingCar.getUserId());

        return existingCar;

    }

}
