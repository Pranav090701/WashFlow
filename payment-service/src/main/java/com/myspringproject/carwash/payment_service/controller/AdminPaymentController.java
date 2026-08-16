package com.myspringproject.carwash.payment_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.payment_service.dto.PaymentResponse;
import com.myspringproject.carwash.payment_service.entity.Payment.PaymentStatus;
import com.myspringproject.carwash.payment_service.service.PaymentService;

@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPaymentsForAdmin());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentForAdmin(paymentId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatusForAdmin(status));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByCustomerForAdmin(customerId));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBookingForAdmin(bookingId));
    }

    @GetMapping("/razorpay-order/{razorpayOrderId}")
    public ResponseEntity<PaymentResponse> getPaymentByRazorpayOrder(@PathVariable String razorpayOrderId) {
        return ResponseEntity.ok(paymentService.getPaymentByRazorpayOrderForAdmin(razorpayOrderId));
    }
}
