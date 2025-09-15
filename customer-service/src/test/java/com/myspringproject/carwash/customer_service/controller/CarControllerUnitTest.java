package com.myspringproject.carwash.customer_service.controller;

import com.myspringproject.carwash.customer_service.dto.CarDTO;
import com.myspringproject.carwash.customer_service.entity.Car;
import com.myspringproject.carwash.customer_service.service.CarService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myspringproject.carwash.customer_service.config.SecurityConfig;

@WebMvcTest(CarController.class)
@Import(SecurityConfig.class)
class CarControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarService carService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addCar_success() throws Exception {
        UUID userId = UUID.randomUUID();
        CarDTO carDTO = new CarDTO("Toyota", "Corolla", "Red", 2020, "ABC123");
        Car car = new Car(UUID.randomUUID(), userId, "Toyota", "Corolla", "Red", 2020, "ABC123");

        Mockito.when(carService.addCar(any(CarDTO.class), eq(userId))).thenReturn(car);

        mockMvc.perform(post("/cars")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "brand": "Toyota",
                        "model": "Corolla",
                        "color": "Red",
                        "year": 2020,
                        "plateNumber": "ABC123"
                    }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.brand").value("Toyota"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCarsByUserId_success() throws Exception {
        UUID userId = UUID.randomUUID();
        Car car = new Car(UUID.randomUUID(), userId, "Toyota", "Corolla", "Red", 2020, "ABC123");
        Mockito.when(carService.getCarsByUserId(userId)).thenReturn(List.of(car));

        mockMvc.perform(get("/cars")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].brand").value("Toyota"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateCar_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        CarDTO carDTO = new CarDTO("Honda", "Civic", "Blue", 2021, "XYZ789");
        Car updatedCar = new Car(carId, userId, "Honda", "Civic", "Blue", 2021, "XYZ789");

        Mockito.when(carService.updateCar(any(CarDTO.class), eq(userId), eq(carId))).thenReturn(updatedCar);

        mockMvc.perform(put("/cars/update/" + carId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "brand": "Honda",
                        "model": "Civic",
                        "color": "Blue",
                        "year": 2021,
                        "plateNumber": "XYZ789"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.brand").value("Honda"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCar_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        Mockito.doNothing().when(carService).deleteCarById(carId, userId);

        mockMvc.perform(delete("/cars/" + carId)
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("Car deleted successfully."));
    }

    @Test
    @WithMockUser(roles = "WASHER") // Authenticated, but not CUSTOMER
    void addCar_forbiddenWithoutRole() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/cars")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "brand": "Toyota",
                        "model": "Corolla",
                        "color": "Red",
                        "year": 2020,
                        "plateNumber": "ABC123"
                    }
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCarsByUserId_emptyList() throws Exception {
        UUID userId = UUID.randomUUID();
        Mockito.when(carService.getCarsByUserId(userId)).thenReturn(List.of());

        mockMvc.perform(get("/cars")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
