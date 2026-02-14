package com.myspringproject.carwash.washer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.myspringproject.carwash.washer_service")
public class WasherServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(WasherServiceApplication.class, args);
	}

}
