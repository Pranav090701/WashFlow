package com.myspringproject.carwash.payment_service.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String recipientEmail,
        Map<String, Object> data) {
}
