package com.myspringproject.carwash.washer_service.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.washer_service.dto.WasherProfileDTO;
import com.myspringproject.carwash.washer_service.entity.WasherProfile;
import com.myspringproject.carwash.washer_service.exception.ProfileAlreadyExistsException;
import com.myspringproject.carwash.washer_service.exception.ProfileNotFoundException;
import com.myspringproject.carwash.washer_service.exception.WasherNotFoundInArea;
import com.myspringproject.carwash.washer_service.repository.WasherProfileRepository;

@Service
public class WasherProfileService {
    
    private final WasherProfileRepository profileRepository;
    private final RedisTemplate<String, List<WasherProfile>> redisTemplate;

    public WasherProfileService(RedisTemplate<String, List<WasherProfile>> redisTemplate,WasherProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
        this.redisTemplate = redisTemplate;
    }

    public WasherProfile createWasherProfile(UUID userId, WasherProfileDTO profileData) {
        if (profileRepository.existsById(userId)) {
            throw new ProfileAlreadyExistsException("WasherProfile already exists for userId " + userId);
        }

        WasherProfile washerProfile = new WasherProfile();
        washerProfile.setUserId(userId);
        washerProfile.setFullName(profileData.getFullName());
        washerProfile.setPhoneNumber(profileData.getPhoneNumber());
        washerProfile.setProfilePictureUrl(profileData.getProfilePictureUrl());
        washerProfile.setServiceArea(profileData.getServiceArea());
        washerProfile.setPincode(profileData.getPincode());
        washerProfile.setExperience(profileData.getExperience());
        washerProfile.setPricing(profileData.getPricing());
        washerProfile.setAvailability(true); // Default to available when created
        washerProfile.setAverageRating(0.0);
        washerProfile.setTotalRatings(0);

        WasherProfile saved = profileRepository.save(washerProfile);
        evictAreaCache(saved.getServiceArea(), saved.getPincode());
        return saved;
    }


    public WasherProfile getWasherProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("WasherProfile not found"));
    }

    
    public WasherProfile updateWasherProfile(UUID userId, WasherProfileDTO updatedWasherProfile) {

        WasherProfile existing = profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("WasherProfile not found for user ID: " + userId));

        String previousServiceArea = existing.getServiceArea();
        String previousPincode = existing.getPincode();

        existing.setFullName(updatedWasherProfile.getFullName());
        existing.setPhoneNumber(updatedWasherProfile.getPhoneNumber());
        existing.setProfilePictureUrl(updatedWasherProfile.getProfilePictureUrl());
        existing.setServiceArea(updatedWasherProfile.getServiceArea());
        existing.setExperience(updatedWasherProfile.getExperience());
        existing.setPincode(updatedWasherProfile.getPincode());
        existing.setPricing(updatedWasherProfile.getPricing());

        WasherProfile saved = profileRepository.save(existing);
        evictAreaCache(previousServiceArea, previousPincode);
        evictAreaCache(saved.getServiceArea(), saved.getPincode());
        return saved;
    }

    
    public String updateAvailabilityStatus(UUID userId, boolean availability){
        WasherProfile existing = profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("WasherProfile not found for user ID: " + userId));

        existing.setAvailability(availability);

        profileRepository.save(existing);
        evictAreaCache(existing.getServiceArea(), existing.getPincode());

        return "Availability Updated to " + availability + " for User " + userId;
    }

    public List<WasherProfile> getAllAvailableWashersInArea(String serviceArea, String pincode) {
        List<WasherProfile> profiles = findWashersByAreaAndPincode(serviceArea, pincode);
        if(profiles == null || profiles.isEmpty()) {
            throw new WasherNotFoundInArea("No available washers found in area: " + serviceArea + ", pincode: " + pincode);
        }
        return profiles.stream()
                .filter(WasherProfile::isAvailability)
                .toList();
    }

    
    public List<WasherProfile> findWashersByAreaAndPincode(String serviceArea, String pincode) {
        String key = "washer_profiles:area:" + serviceArea + ":pincode:" + pincode;
        List<WasherProfile> profiles = redisTemplate.opsForValue().get(key);
        if(profiles == null){
            profiles = profileRepository.findByServiceAreaAndPincode(serviceArea, pincode);
            redisTemplate.opsForValue().set(key, profiles, Duration.ofMinutes(30));
        }
        return profiles;
    }

    public void updateAverageRating(UUID washerId, int newRatingScore) {
        WasherProfile profile = profileRepository.findById(washerId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        int totalRatings = profile.getTotalRatings(); // Add these fields
        double currentAvg = profile.getAverageRating();

        double updatedAvg = ((currentAvg * totalRatings) + newRatingScore) / (totalRatings + 1);

        profile.setAverageRating(updatedAvg);
        profile.setTotalRatings(totalRatings + 1);
        profileRepository.save(profile);
    }

    private void updateCachedWasherAvailability(UUID washerId, String serviceArea, String pincode, boolean availability) {
        String key = "washer_profiles:area:" + serviceArea + ":pincode:" + pincode;
        List<WasherProfile> cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            cached.stream()
                .filter(w -> w.getUserId().equals(washerId))
                .findFirst()
                .ifPresent(w -> w.setAvailability(availability));
            redisTemplate.opsForValue().set(key, cached, Duration.ofMinutes(30));
    }
}

    private void evictAreaCache(String serviceArea, String pincode) {
        if (serviceArea == null || pincode == null) {
            return;
        }
        redisTemplate.delete("washer_profiles:area:" + serviceArea + ":pincode:" + pincode);
    }

    public List<UUID> getAvailableWashersId() {
        return profileRepository.findByAvailability(true)
                .stream()
                .map(WasherProfile::getUserId)
                .toList();
    }
}
