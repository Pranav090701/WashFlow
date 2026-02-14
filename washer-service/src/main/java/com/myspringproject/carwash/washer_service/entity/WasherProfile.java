package com.myspringproject.carwash.washer_service.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "washer_profile")
public class WasherProfile {

    @Id
    @Column(nullable = false, unique = true)
    private UUID userId;

    private String fullName;
    private String phoneNumber;
    private String profilePictureUrl;

    private String serviceArea;
    private String pincode;
    private int experience; // in years
    private double pricing; // base price
    private boolean availability; // e.g., Weekends, 10am–6pm

    private double averageRating;
    private int totalRatings;

    public UUID getUserId() {
        return userId;
    }
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    public String getServiceArea() {
        return serviceArea;
    }
    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }
    public String getPincode() {
        return pincode;
    }
    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
    public int getExperience() {
        return experience;
    }
    public void setExperience(int experience) {
        this.experience = experience;
    }
    public double getPricing() {
        return pricing;
    }
    public void setPricing(double pricing) {
        this.pricing = pricing;
    }

    public boolean isAvailability() {
        return availability;
    }
    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
    
    public double getAverageRating() {
        return averageRating;
    }
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
    public int getTotalRatings() {
        return totalRatings;
    }
    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }
    
    @Override
    public String toString() {
        return "WasherProfile [userId=" + userId + ", fullName=" + fullName + ", phoneNumber=" + phoneNumber
                + ", profilePictureUrl=" + profilePictureUrl + ", serviceArea=" + serviceArea + ", pincode=" + pincode
                + ", experience=" + experience + ", pricing=" + pricing + ", availability=" + availability
                + ", averageRating=" + averageRating + ", totalRatings=" + totalRatings + "]";
    }
    
}






