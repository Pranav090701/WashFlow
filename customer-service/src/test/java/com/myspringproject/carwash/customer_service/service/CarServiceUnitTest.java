package com.myspringproject.carwash.customer_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.myspringproject.carwash.customer_service.dto.CarDTO;
import com.myspringproject.carwash.customer_service.entity.Car;
import com.myspringproject.carwash.customer_service.exception.CarNotFoundException;
import com.myspringproject.carwash.customer_service.exception.CarOwnershipMismatchException;
import com.myspringproject.carwash.customer_service.repository.CarRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.test.context.support.WithMockUser;

class CarServiceUnitTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private CarService carService;

    private UUID userId;
    private UUID carId;
    private CarDTO carDTO;
    private Car car;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        carId = UUID.randomUUID();
        carDTO = new CarDTO("Toyota", "Corolla", "Red", 2020, "ABC123");
        car = new Car(carId, userId, "Toyota", "Corolla", "Red", 2020, "ABC123");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addCar_success() {
        when(carRepository.save(any(Car.class))).thenReturn(car);

        Car result = carService.addCar(carDTO, userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(redisTemplate).delete("cars:user:" + userId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCarsByUserId_cacheHit() {
        List<Car> cachedCars = List.of(car);

        // Mock ValueOperations
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cars:user:" + userId)).thenReturn(cachedCars);

        List<Car> result = carService.getCarsByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(carId, result.get(0).getId());
        verify(carRepository, never()).findByUserId(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCarsByUserId_cacheMiss() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cars:user:" + userId)).thenReturn(null);

        when(carRepository.findByUserId(userId)).thenReturn(List.of(car));

        List<Car> result = carService.getCarsByUserId(userId);

        assertEquals(1, result.size());
        verify(redisTemplate, times(2)).opsForValue(); // <-- Fix: expect 2 calls
        verify(carRepository).findByUserId(userId);
        verify(valueOperations).set("cars:user:" + userId, List.of(car));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCarById_success() {
        when(carRepository.findById(carId)).thenReturn(Optional.of(car));

        carService.deleteCarById(carId, userId);

        verify(carRepository).delete(car);
        verify(redisTemplate).delete("cars:user:" + userId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCarById_notFound_throwsException() {
        when(carRepository.findById(carId)).thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class, () -> {
            carService.deleteCarById(carId, userId);
        });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCarById_ownershipMismatch_throwsException() {
        Car otherCar = new Car(carId, UUID.randomUUID(), "Honda", "Civic", "Blue", 2019, "XYZ789");
        when(carRepository.findById(carId)).thenReturn(Optional.of(otherCar));

        assertThrows(CarOwnershipMismatchException.class, () -> {
            carService.deleteCarById(carId, userId);
        });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCar_success() {
        when(carRepository.findById(carId)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenReturn(car);

        CarDTO updatedDTO = new CarDTO("Honda", "Civic", "Blue", 2021, "XYZ789");
        Car updated = carService.updateCar(updatedDTO, userId, carId);

        assertEquals("Honda", updated.getBrand());
        assertEquals("Civic", updated.getModel());
        assertEquals("Blue", updated.getColor());
        assertEquals(2021, updated.getYear());
        assertEquals("XYZ789", updated.getPlateNumber());
        verify(redisTemplate).delete("cars:user:" + userId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCar_notFound_throwsException() {
        when(carRepository.findById(carId)).thenReturn(Optional.empty());

        CarDTO updatedDTO = new CarDTO("Honda", "Civic", "Blue", 2021, "XYZ789");
        assertThrows(CarNotFoundException.class, () -> {
            carService.updateCar(updatedDTO, userId, carId);
        });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCar_ownershipMismatch_throwsException() {
        Car otherCar = new Car(carId, UUID.randomUUID(), "Honda", "Civic", "Blue", 2019, "XYZ789");
        when(carRepository.findById(carId)).thenReturn(Optional.of(otherCar));

        CarDTO updatedDTO = new CarDTO("Honda", "Civic", "Blue", 2021, "XYZ789");
        assertThrows(CarOwnershipMismatchException.class, () -> {
            carService.updateCar(updatedDTO, userId, carId);
        });
    }
}
