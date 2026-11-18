package nmk_backend.controller;

import nmk_backend.dto.VerifyPaymentRequest;
import nmk_backend.entity.Order;
import nmk_backend.exception.PaymentConfigurationException;
import nmk_backend.exception.PaymentConfirmationConflictException;
import nmk_backend.repository.OrderRepository;
import nmk_backend.service.PaymentVerificationService;
import nmk_backend.service.RazorpayService;
import nmk_backend.service.SmsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class RazorpayController {

    private static final Logger log =
            LoggerFactory.getLogger(RazorpayController.class);

    private final RazorpayService razorpayService;
    private final OrderRepository orderRepository;
    private final PaymentVerificationService paymentVerificationService;
    private final SmsService smsService;
    private final String publicUrl;

    public RazorpayController(
            RazorpayService razorpayService,
            OrderRepository orderRepository,
            PaymentVerificationService paymentVerificationService,
            SmsService smsService,
            @Value("${app.public-url:http://localhost:5173}") String publicUrl) {

        this.razorpayService = razorpayService;
        this.orderRepository = orderRepository;
        this.paymentVerificationService = paymentVerificationService;
        this.smsService = smsService;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody VerifyPaymentRequest request) {

        if (request == null
                || isBlank(request.getRazorpayOrderId())
                || isBlank(request.getRazorpayPaymentId())
                || isBlank(request.getRazorpaySignature())) {

            return errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay order ID, payment ID and signature are required."
            );
        }

        String razorpayOrderId = request.getRazorpayOrderId();
        String razorpayPaymentId = request.getRazorpayPaymentId();

        // 1. Verify the checkout signature.
        final boolean signatureValid;

        try {
            signatureValid = razorpayService.verifySignature(
                    razorpayOrderId,
                    razorpayPaymentId,
                    request.getRazorpaySignature()
            );
        } catch (PaymentConfigurationException e) {
            log.error("Razorpay payment configuration is unavailable.");
            return errorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment service is temporarily unavailable."
            );
        } catch (Exception e) {
            log.error("Razorpay signature verification could not be completed.", e);
            return errorResponse(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify payment with the payment provider. Please retry."
            );
        }

        if (!signatureValid) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Payment signature verification failed."
            );
        }

        // 2. Find the corresponding database order.
        Order order = orderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (order == null) {
            return errorResponse(
                    HttpStatus.NOT_FOUND,
                    "Order not found."
            );
        }

        // 3. Calculate the expected payment amount in paise.
        final int expectedAmount;

        try {
            expectedAmount = order.getTotalAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .intValueExact();
        } catch (ArithmeticException | NullPointerException e) {
            log.error(
                    "Invalid stored payment amount for Razorpay order {}",
                    razorpayOrderId,
                    e
            );

            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to verify the stored order amount."
            );
        }

        // 4. Confirm capture, order ID, currency and amount with Razorpay.
        final boolean captured;

        try {
            captured = razorpayService.isPaymentCapturedForOrder(
                    razorpayPaymentId,
                    razorpayOrderId,
                    expectedAmount
            );
        } catch (PaymentConfigurationException e) {
            log.error("Razorpay payment configuration is unavailable.");
            return errorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment service is temporarily unavailable."
            );
        } catch (Exception e) {
            log.error(
                    "Razorpay capture verification could not be completed for order {}",
                    razorpayOrderId,
                    e
            );

            return errorResponse(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to confirm payment with the payment provider. Please retry."
            );
        }

        if (!captured) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Payment is not captured or the payment details do not match."
            );
        }

        try {
            // 5. Confirm payment inside the locked database transaction.
            PaymentVerificationService.ConfirmationResult result =
                    paymentVerificationService.confirmPayment(
                            razorpayOrderId,
                            razorpayPaymentId
                    );

            Order confirmedOrder = result.order();

            if (confirmedOrder == null
                    || isBlank(confirmedOrder.getTrackingToken())
                    || confirmedOrder.getId() == null) {

                log.error(
                        "Payment confirmation returned incomplete order data for {}",
                        razorpayOrderId
                );

                return errorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment was received, but order tracking details require support."
                );
            }

            String trackingUrl = trackingUrl(confirmedOrder);

            // 6. Send SMS only for the first successful confirmation.
            if (result.newlyConfirmed()) {
                try {
                    smsService.sendOrderNotification(
                            confirmedOrder.getPhone(),
                            String.valueOf(confirmedOrder.getId()),
                            "CONFIRMED",
                            "",
                            "",
                            trackingUrl
                    );
                } catch (Exception smsException) {
                    log.warn(
                            "Payment succeeded, but confirmation SMS failed for order {}",
                            confirmedOrder.getId(),
                            smsException
                    );
                }
            }

            // 7. Return the existing successful response contract.
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("verified", true);
            response.put(
                    "message",
                    result.newlyConfirmed()
                            ? "Payment verified and order confirmed"
                            : "Order already paid"
            );
            response.put("databaseOrderId", confirmedOrder.getId());
            response.put("trackingToken", confirmedOrder.getTrackingToken());
            response.put("trackingUrl", trackingUrl);

            return ResponseEntity.ok(response);

        } catch (PaymentConfirmationConflictException e) {
            log.warn(
                    "Payment confirmation conflict for Razorpay order {}: {}",
                    razorpayOrderId,
                    e.getMessage()
            );

            return errorResponse(
                    HttpStatus.CONFLICT,
                    e.getMessage()
            );

        } catch (PaymentConfigurationException e) {
            log.error("Razorpay payment configuration is unavailable.");
            return errorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment service is temporarily unavailable."
            );


        } catch (Exception e) {
            log.error(
                    "Unexpected payment confirmation failure for Razorpay order {}",
                    razorpayOrderId,
                    e
            );

            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment verification could not be completed. Contact support if money was deducted."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status,
            String message) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("verified", false);
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }

    private String trackingUrl(Order order) {
        return publicUrl + "/track/" + order.getTrackingToken();
    }
}