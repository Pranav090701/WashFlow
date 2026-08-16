package com.myspringproject.carwash.payment_service.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.myspringproject.carwash.payment_service.entity.Payment;
import com.myspringproject.carwash.payment_service.entity.Payment.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findFirstByCustomerIdAndWasherIdAndDateAndSlotTimeAndStatusInOrderByCreatedAtDesc(
            UUID customerId,
            UUID washerId,
            LocalDate date,
            LocalTime slotTime,
            Collection<PaymentStatus> statuses);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByCustomerId(UUID customerId);

    List<Payment> findByBookingId(UUID bookingId);
}
