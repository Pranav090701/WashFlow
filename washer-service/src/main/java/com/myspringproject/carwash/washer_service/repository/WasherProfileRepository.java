package com.myspringproject.carwash.washer_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.myspringproject.carwash.washer_service.entity.WasherProfile;

@Repository
public interface WasherProfileRepository extends JpaRepository<WasherProfile, UUID> {
    public List<WasherProfile> findByServiceAreaAndPincode(String serviceArea, String pincode);
    public List<WasherProfile> findByAvailability(boolean availability);
}
