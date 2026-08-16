package com.myspringproject.carwash.notification_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.notification_service.dto.NotificationEvent;

@Service
public class NotificationMailService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationMailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NotificationMailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(NotificationEvent event) {
        if (event.recipientEmail() == null || event.recipientEmail().isBlank()) {
            logger.warn("Skipping event {} because recipient email is missing", event.eventId());
            return;
        }

        EmailContent content = buildContent(event);
        if (content == null) {
            logger.info("Ignoring notification event type {}", event.eventType());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.recipientEmail());
        message.setSubject(content.subject());
        message.setText(content.body());

        try {
            mailSender.send(message);
            logger.info("Sent {} email to {}", event.eventType(), event.recipientEmail());
        } catch (MailException e) {
            logger.error("Failed to send {} email to {}", event.eventType(), event.recipientEmail(), e);
            throw e;
        }
    }

    private EmailContent buildContent(NotificationEvent event) {
        Map<String, Object> data = event.data() == null ? Map.of() : event.data();

        return switch (event.eventType()) {
            case "auth.user.registered" -> new EmailContent(
                    "Verify your Car Wash account",
                    "Welcome to Car Wash App.\n\n"
                            + "Verify your email using this link:\n"
                            + stringValue(data, "verificationLink")
                            + "\n\nThis link expires in 3 hours.");
            case "payment.success" -> new EmailContent(
                    "Car Wash payment successful",
                    "Your payment was successful.\n\n"
                            + "Amount: " + stringValue(data, "amount") + " " + stringValue(data, "currency") + "\n"
                            + "Booking ID: " + stringValue(data, "bookingId") + "\n"
                            + "Washer ID: " + stringValue(data, "washerId") + "\n"
                            + "Slot: " + stringValue(data, "date") + " " + stringValue(data, "slotTime"));
            case "payment.failed" -> new EmailContent(
                    "Car Wash payment failed",
                    "Your payment could not be completed.\n\n"
                            + "Reason: " + stringValue(data, "failureReason") + "\n"
                            + "Slot: " + stringValue(data, "date") + " " + stringValue(data, "slotTime"));
            case "booking.confirmed" -> new EmailContent(
                    "Car Wash booking confirmed",
                    "Your booking is confirmed.\n\n"
                            + "Booking ID: " + stringValue(data, "bookingId") + "\n"
                            + "Slot: " + stringValue(data, "date") + " " + stringValue(data, "slotTime"));
            case "booking.completed" -> new EmailContent(
                    "Car Wash booking completed",
                    "Your booking has been marked completed.\n\n"
                            + "Booking ID: " + stringValue(data, "bookingId"));
            default -> null;
        };
    }

    private String stringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "N/A" : value.toString();
    }

    private record EmailContent(String subject, String body) {
    }
}
