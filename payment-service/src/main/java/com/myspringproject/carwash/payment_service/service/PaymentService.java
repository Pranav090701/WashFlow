package com.myspringproject.carwash.payment_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myspringproject.carwash.payment_service.dto.InitiatePaymentRequest;
import com.myspringproject.carwash.payment_service.dto.LockedSlotQuoteResponse;
import com.myspringproject.carwash.payment_service.dto.PaymentInitiationResponse;
import com.myspringproject.carwash.payment_service.dto.PaymentResponse;
import com.myspringproject.carwash.payment_service.dto.VerifyPaymentRequest;
import com.myspringproject.carwash.payment_service.entity.Payment;
import com.myspringproject.carwash.payment_service.entity.Payment.PaymentStatus;
import com.myspringproject.carwash.payment_service.event.PaymentEventPublisher;
import com.myspringproject.carwash.payment_service.exception.BookingConfirmationException;
import com.myspringproject.carwash.payment_service.exception.PaymentAccessDeniedException;
import com.myspringproject.carwash.payment_service.exception.PaymentNotFoundException;
import com.myspringproject.carwash.payment_service.exception.PaymentVerificationException;
import com.myspringproject.carwash.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final BookingClientService bookingClientService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;

    private static final List<PaymentStatus> ACTIVE_PAYMENT_STATUSES = List.of(
            PaymentStatus.INITIATED,
            PaymentStatus.SUCCESS,
            PaymentStatus.BOOKING_CONFIRM_FAILED);

    public PaymentService(
            PaymentRepository paymentRepository,
            RazorpayClient razorpayClient,
            BookingClientService bookingClientService,
            PaymentEventPublisher paymentEventPublisher,
            @Value("${razorpay.key-id}") String razorpayKeyId,
            @Value("${razorpay.key-secret}") String razorpayKeySecret,
            @Value("${razorpay.webhook-secret}") String razorpayWebhookSecret) {
        this.paymentRepository = paymentRepository;
        this.razorpayClient = razorpayClient;
        this.bookingClientService = bookingClientService;
        this.paymentEventPublisher = paymentEventPublisher;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    @Transactional(noRollbackFor = BookingConfirmationException.class)
    public void handleRazorpayWebhook(String payload, String razorpaySignature) {
        if (!isWebhookSignatureValid(payload, razorpaySignature)) {
            throw new PaymentVerificationException("Invalid Razorpay webhook signature");
        }

        JSONObject eventPayload = new JSONObject(payload);
        String eventName = eventPayload.optString("event");
        JSONObject paymentEntity = extractPaymentEntity(eventPayload);

        if (paymentEntity == null) {
            logger.info("Ignoring Razorpay webhook without payment entity: {}", eventName);
            return;
        }

        String orderId = paymentEntity.optString("order_id", null);
        String razorpayPaymentId = paymentEntity.optString("id", null);

        if (orderId == null || orderId.isBlank()) {
            logger.info("Ignoring Razorpay webhook without order id: {}", eventName);
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElse(null);
        if (payment == null) {
            logger.info("Ignoring Razorpay webhook for unknown order id: {}", orderId);
            return;
        }

        if ("order.paid".equals(eventName) || "payment.captured".equals(eventName)) {
            handleCapturedWebhookPayment(payment, razorpayPaymentId, paymentEntity);
            return;
        }

        if ("payment.failed".equals(eventName)) {
            handleFailedWebhookPayment(payment, razorpayPaymentId, paymentEntity);
            return;
        }

        logger.info("Ignoring unsupported Razorpay webhook event: {}", eventName);
    }

    @Transactional
    public PaymentInitiationResponse initiatePayment(UUID customerId, String customerEmail, InitiatePaymentRequest request) {
        var activePayment = paymentRepository
                .findFirstByCustomerIdAndWasherIdAndDateAndSlotTimeAndStatusInOrderByCreatedAtDesc(
                        customerId,
                        request.getWasherId(),
                        request.getDate(),
                        request.getSlotTime(),
                        ACTIVE_PAYMENT_STATUSES);
        if (activePayment.isPresent()) {
            Payment payment = activePayment.get();
            if ((payment.getCustomerEmail() == null || payment.getCustomerEmail().isBlank())
                    && customerEmail != null && !customerEmail.isBlank()) {
                payment.setCustomerEmail(customerEmail);
                paymentRepository.save(payment);
            }
            return toInitiationResponse(payment);
        }

        LockedSlotQuoteResponse quote = bookingClientService.getLockedSlotQuote(customerId, request);
        BigDecimal amount = quote.amount().setScale(2, RoundingMode.HALF_UP);
        Long amountSubunits = toSubunits(amount);
        String receipt = "cw_" + UUID.randomUUID();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountSubunits);
        orderRequest.put("currency", quote.currency());
        orderRequest.put("receipt", receipt);

        JSONObject notes = new JSONObject();
        notes.put("customerId", customerId.toString());
        notes.put("washerId", request.getWasherId().toString());
        notes.put("date", request.getDate().toString());
        notes.put("slotTime", request.getSlotTime().toString());
        orderRequest.put("notes", notes);

        Order order;
        try {
            order = razorpayClient.orders.create(orderRequest);
        } catch (RazorpayException e) {
            throw new PaymentVerificationException("Unable to create Razorpay order: " + e.getMessage());
        }

        Payment payment = new Payment();
        payment.setCustomerId(customerId);
        payment.setCustomerEmail(customerEmail);
        payment.setWasherId(request.getWasherId());
        payment.setDate(request.getDate());
        payment.setSlotTime(request.getSlotTime());
        payment.setAmount(amount);
        payment.setAmountSubunits(amountSubunits);
        payment.setCurrency(quote.currency());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setRazorpayOrderId(order.get("id"));

        Payment saved = paymentRepository.save(payment);
        return toInitiationResponse(saved);
    }

    @Transactional(noRollbackFor = BookingConfirmationException.class)
    public PaymentResponse verifyPayment(UUID customerId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + request.getPaymentId()));

        ensureCustomerOwnsPayment(payment, customerId);
        ensureOrderMatches(payment, request);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return toResponse(payment);
        }
        ensurePaymentCanBeVerified(payment);
        ensurePaymentIdMatchesRetry(payment, request);

        if (!isSignatureValid(request)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay signature verification failed");
            Payment failedPayment = paymentRepository.save(payment);
            paymentEventPublisher.publishPaymentFailed(failedPayment);
            return toResponse(failedPayment);
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());

        try {
            UUID bookingId = bookingClientService.confirmBooking(payment);
            payment.setBookingId(bookingId);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            Payment savedPayment = paymentRepository.save(payment);
            paymentEventPublisher.publishPaymentSuccess(savedPayment);
            return toResponse(savedPayment);
        } catch (RuntimeException e) {
            payment.setStatus(PaymentStatus.BOOKING_CONFIRM_FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw new BookingConfirmationException("Payment verified, but booking confirmation failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId, UUID requesterId, String role) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (!"ADMIN".equalsIgnoreCase(role)) {
            ensureCustomerOwnsPayment(payment, requesterId);
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPaymentsForAdmin() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentForAdmin(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByRazorpayOrderForAdmin(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for Razorpay order: " + razorpayOrderId));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatusForAdmin(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomerForAdmin(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBookingForAdmin(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Long toSubunits(BigDecimal amount) {
        return amount.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private void ensureCustomerOwnsPayment(Payment payment, UUID customerId) {
        if (!payment.getCustomerId().equals(customerId)) {
            throw new PaymentAccessDeniedException("Payment does not belong to customer " + customerId);
        }
    }

    private void ensurePaymentCanBeVerified(Payment payment) {
        if (payment.getStatus() != PaymentStatus.INITIATED
                && payment.getStatus() != PaymentStatus.BOOKING_CONFIRM_FAILED) {
            throw new PaymentVerificationException("Payment cannot be verified from status " + payment.getStatus());
        }
    }

    private void ensurePaymentIdMatchesRetry(Payment payment, VerifyPaymentRequest request) {
        if (payment.getRazorpayPaymentId() != null
                && !payment.getRazorpayPaymentId().equals(request.getRazorpayPaymentId())) {
            throw new PaymentVerificationException("Razorpay payment id does not match payment record");
        }
    }

    private void ensureOrderMatches(Payment payment, VerifyPaymentRequest request) {
        if (!payment.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new PaymentVerificationException("Razorpay order id does not match payment record");
        }
    }

    private boolean isSignatureValid(VerifyPaymentRequest request) {
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String generatedSignature = hmacSha256(payload, razorpayKeySecret);
        return MessageDigest.isEqual(
                generatedSignature.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isWebhookSignatureValid(String payload, String razorpaySignature) {
        String generatedSignature = hmacSha256(payload, razorpayWebhookSecret);
        return MessageDigest.isEqual(
                generatedSignature.getBytes(StandardCharsets.UTF_8),
                razorpaySignature.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject extractPaymentEntity(JSONObject eventPayload) {
        JSONObject payload = eventPayload.optJSONObject("payload");
        if (payload == null) {
            return null;
        }

        JSONObject payment = payload.optJSONObject("payment");
        if (payment == null) {
            return null;
        }

        return payment.optJSONObject("entity");
    }

    private void handleCapturedWebhookPayment(
            Payment payment,
            String razorpayPaymentId,
            JSONObject paymentEntity) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        ensureWebhookAmountMatches(payment, paymentEntity);
        setRazorpayPaymentIdIfPresent(payment, razorpayPaymentId);

        try {
            UUID bookingId = bookingClientService.confirmBooking(payment);
            payment.setBookingId(bookingId);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            Payment savedPayment = paymentRepository.save(payment);
            paymentEventPublisher.publishPaymentSuccess(savedPayment);
        } catch (RuntimeException e) {
            payment.setStatus(PaymentStatus.BOOKING_CONFIRM_FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw new BookingConfirmationException("Webhook payment verified, but booking confirmation failed", e);
        }
    }

    private void handleFailedWebhookPayment(
            Payment payment,
            String razorpayPaymentId,
            JSONObject paymentEntity) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        setRazorpayPaymentIdIfPresent(payment, razorpayPaymentId);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(paymentEntity.optString("error_description", "Razorpay payment failed"));
        Payment savedPayment = paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentFailed(savedPayment);
    }

    private void ensureWebhookAmountMatches(Payment payment, JSONObject paymentEntity) {
        long webhookAmount = paymentEntity.optLong("amount", -1);
        String webhookCurrency = paymentEntity.optString("currency", "");
        if (webhookAmount != payment.getAmountSubunits()
                || !payment.getCurrency().equalsIgnoreCase(webhookCurrency)) {
            throw new PaymentVerificationException("Razorpay webhook amount or currency does not match payment record");
        }
    }

    private void setRazorpayPaymentIdIfPresent(Payment payment, String razorpayPaymentId) {
        if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
            if (payment.getRazorpayPaymentId() != null
                    && !payment.getRazorpayPaymentId().equals(razorpayPaymentId)) {
                throw new PaymentVerificationException("Razorpay payment id does not match payment record");
            }
            payment.setRazorpayPaymentId(razorpayPaymentId);
        }
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to verify Razorpay signature", e);
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomerId(),
                payment.getWasherId(),
                payment.getDate(),
                payment.getSlotTime(),
                payment.getAmount(),
                payment.getAmountSubunits(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getBookingId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    private PaymentInitiationResponse toInitiationResponse(Payment payment) {
        return new PaymentInitiationResponse(
                payment.getId(),
                payment.getRazorpayOrderId(),
                payment.getAmountSubunits(),
                payment.getCurrency(),
                razorpayKeyId,
                payment.getStatus());
    }
}
