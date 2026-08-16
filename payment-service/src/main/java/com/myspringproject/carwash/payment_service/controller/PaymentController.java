package com.myspringproject.carwash.payment_service.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myspringproject.carwash.payment_service.dto.InitiatePaymentRequest;
import com.myspringproject.carwash.payment_service.dto.PaymentInitiationResponse;
import com.myspringproject.carwash.payment_service.dto.PaymentResponse;
import com.myspringproject.carwash.payment_service.dto.VerifyPaymentRequest;
import com.myspringproject.carwash.payment_service.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitiatePaymentRequest request) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        PaymentInitiationResponse response = paymentService.initiatePayment(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VerifyPaymentRequest request) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(paymentService.verifyPayment(customerId, request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature,
            @RequestBody String payload) {
        paymentService.handleRazorpayWebhook(payload, razorpaySignature);
        return ResponseEntity.ok("Webhook processed");
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        return ResponseEntity.ok(paymentService.getPayment(paymentId, requesterId, role));
    }
}
