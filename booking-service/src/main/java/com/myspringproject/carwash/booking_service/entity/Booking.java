package com.myspringproject.carwash.booking_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID slotId; // FK to Slot

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private BookingStatus status; 

    @Column(nullable = false)
    private double price;

    private LocalDateTime paymentTime;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BookingStatus {
        PENDING_PAYMENT,
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }

    
    /*
     * No args constructor 
     */
    public Booking() {

    }

    public Booking(UUID slotId, UUID userId, BookingStatus status, double price, LocalDateTime paymentTime,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.slotId = slotId;
        this.userId = userId;
        this.status = status;
        this.price = price;
        this.paymentTime = paymentTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public void setSlotId(UUID slotId) {
        this.slotId = slotId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Booking [id=" + id + ", slotId=" + slotId + ", userId=" + userId + ", status=" + status + ", price="
                + price + ", paymentTime=" + paymentTime + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt
                + "]";
    }

}
