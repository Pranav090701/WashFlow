package com.myspringproject.carwash.payment_service.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.myspringproject.carwash.payment_service.entity.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchangeName;

    public PaymentEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${carwash.events.exchange}") String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchangeName = exchangeName;
    }

    public void publishPaymentSuccess(Payment payment) {
        publish("payment.success", payment, payment.getFailureReason());
    }

    public void publishPaymentFailed(Payment payment) {
        publish("payment.failed", payment, payment.getFailureReason());
    }

    private void publish(String eventType, Payment payment, String failureReason) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                payment.getCustomerEmail(),
                Map.ofEntries(
                        Map.entry("paymentId", payment.getId().toString()),
                        Map.entry("customerId", payment.getCustomerId().toString()),
                        Map.entry("washerId", payment.getWasherId().toString()),
                        Map.entry("date", payment.getDate().toString()),
                        Map.entry("slotTime", payment.getSlotTime().toString()),
                        Map.entry("amount", payment.getAmount().toPlainString()),
                        Map.entry("currency", payment.getCurrency()),
                        Map.entry("razorpayOrderId", payment.getRazorpayOrderId()),
                        Map.entry("bookingId", payment.getBookingId() == null ? "N/A" : payment.getBookingId().toString()),
                        Map.entry("failureReason", failureReason == null ? "N/A" : failureReason)));

        try {
            rabbitTemplate.convertAndSend(exchangeName, eventType, objectMapper.writeValueAsString(event));
            logger.info("Published {} event for payment {}", eventType, payment.getId());
        } catch (AmqpException | JsonProcessingException e) {
            logger.warn("Unable to publish {} event for payment {}", eventType, payment.getId(), e);
        }
    }
}
