package com.myspringproject.carwash.payment_service.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import com.myspringproject.carwash.payment_service.dto.BookingRequestDto;
import com.myspringproject.carwash.payment_service.dto.BookingResponseDto;
import com.myspringproject.carwash.payment_service.dto.InitiatePaymentRequest;
import com.myspringproject.carwash.payment_service.dto.LockedSlotQuoteResponse;
import com.myspringproject.carwash.payment_service.entity.Payment;
import com.myspringproject.carwash.payment_service.exception.PaymentSlotUnavailableException;

@Service
public class BookingClientService {

    private final WebClient bookingWebClient;
    private final String bookingConfirmationToken;

    public BookingClientService(WebClient bookingWebClient,
                                @Value("${carwash.internal.booking-confirmation-token}") String bookingConfirmationToken) {
        this.bookingWebClient = bookingWebClient;
        this.bookingConfirmationToken = bookingConfirmationToken;
    }

    public LockedSlotQuoteResponse getLockedSlotQuote(UUID customerId, InitiatePaymentRequest request) {
        BookingRequestDto quoteRequest = new BookingRequestDto(
                request.getWasherId(),
                request.getSlotTime(),
                request.getDate());

        LockedSlotQuoteResponse response;
        try {
            response = bookingWebClient.post()
                    .uri("/slots/locked-quote")
                    .header("X-Customer-Id", customerId.toString())
                    .header("X-Internal-Service-Token", bookingConfirmationToken)
                    .bodyValue(quoteRequest)
                    .retrieve()
                    .bodyToMono(LockedSlotQuoteResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new PaymentSlotUnavailableException("Slot lock expired or not found", e);
            }
            throw e;
        }

        if (response == null || response.amount() == null || response.currency() == null) {
            throw new IllegalStateException("Booking service did not return a locked slot quote");
        }
        return response;
    }

    public UUID confirmBooking(Payment payment) {
        BookingRequestDto request = new BookingRequestDto(
                payment.getWasherId(),
                payment.getSlotTime(),
                payment.getDate());

        BookingResponseDto response = bookingWebClient.post()
                .uri("/slots/confirm")
                .header("X-Customer-Id", payment.getCustomerId().toString())
                .header("X-Internal-Service-Token", bookingConfirmationToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BookingResponseDto.class)
                .block();

        if (response == null || response.getId() == null) {
            throw new IllegalStateException("Booking service did not return a booking id");
        }
        return response.getId();
    }
}
