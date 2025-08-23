package com.myspringproject.carwash.customer_service.repository;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.myspringproject.carwash.customer_service.entity.CustomerProfile;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

}
