package com.myspringproject.carwash.payment_service.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.payment_service.exception.PaymentDependencyUnavailableException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class RazorpayOrderService {

    private final RazorpayClient razorpayClient;

    public RazorpayOrderService(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    @CircuitBreaker(name = "paymentRazorpayOrder", fallbackMethod = "createOrderFallback")
    public Order createOrder(JSONObject orderRequest) throws RazorpayException {
        return razorpayClient.orders.create(orderRequest);
    }

    public Order createOrderFallback(JSONObject orderRequest, Throwable cause) {
        throw new PaymentDependencyUnavailableException("Razorpay order service is temporarily unavailable", cause);
    }
}
