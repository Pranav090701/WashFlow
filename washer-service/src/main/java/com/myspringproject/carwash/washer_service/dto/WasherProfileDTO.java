package com.myspringproject.carwash.washer_service.dto;

public class WasherProfileDTO {

    private String fullName;
    private String phoneNumber;
    private String profilePictureUrl;

    private String serviceArea;
    private String pincode;
    private int experience; // in years
    private double pricing; // base price
    
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

    

}
