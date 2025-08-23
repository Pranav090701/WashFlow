package com.myspringproject.carwash.customer_service.dto;

public class CustomerProfileDTO {
    
    private String fullName;

    private String phoneNumber;

    private String profilePictureUrl;

    public CustomerProfileDTO(String fullName, String phoneNumber, String profilePictureUrl) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
    }

    public CustomerProfileDTO() {
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    
}
