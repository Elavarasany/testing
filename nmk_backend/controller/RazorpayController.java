package nmk_backend.controller;

import nmk_backend.dto.VerifyPaymentRequest;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;
import nmk_backend.service.PaymentVerificationService;
import nmk_backend.service.RazorpayService;
import nmk_backend.service.SmsService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class RazorpayController {

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

        try {
            // 1. Verify Razorpay signature.
            boolean signatureValid = razorpayService.verifySignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );

            if (!signatureValid) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "verified", false,
                                "message", "Payment signature verification failed"
                        )
                );
            }

            // 2. Find the corresponding database order.
            Order order = orderRepository
                    .findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Order not found")
                    );

            // 3. Calculate the expected payment amount.
            int expectedAmount = order.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .intValueExact();

            // 4. Confirm payment is captured for this order and amount.
            boolean captured =
                    razorpayService.isPaymentCapturedForOrder(
                            request.getRazorpayPaymentId(),
                            request.getRazorpayOrderId(),
                            expectedAmount
                    );

            if (!captured) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "verified", false,
                                "message", "Payment is not captured or amount does not match"
                        )
                );
            }

            // 5. Confirm payment inside a locked transaction.
            PaymentVerificationService.ConfirmationResult result =
                    paymentVerificationService.confirmPayment(
                            request.getRazorpayOrderId(),
                            request.getRazorpayPaymentId()
                    );

            Order confirmedOrder = result.order();

            // 6. Send SMS only for the first successful confirmation.
            // Never send duplicate confirmation SMS on repeated requests.
            if (result.newlyConfirmed()) {
                try {
                    smsService.sendOrderNotification(
                            confirmedOrder.getPhone(),
                            String.valueOf(confirmedOrder.getId()),
                            "CONFIRMED",
                            "",
                            "",
                            trackingUrl(confirmedOrder)
                    );
                } catch (Exception smsException) {
                    System.err.println(
                            "Payment succeeded, but SMS failed for order "
                                    + confirmedOrder.getId()
                                    + ": " + smsException.getMessage()
                    );
                }
            }

            // 7. Return tracking information.
            return ResponseEntity.ok(
                    Map.of(
                            "verified", true,
                            "message", result.newlyConfirmed()
                                    ? "Payment verified and order confirmed"
                                    : "Order already paid",
                            "databaseOrderId", confirmedOrder.getId(),
                            "trackingToken", confirmedOrder.getTrackingToken(),
                            "trackingUrl", trackingUrl(confirmedOrder)
                    )
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(
                    Map.of(
                            "verified", false,
                            "message", e.getMessage()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "verified", false,
                            "message", "Payment verification failed"
                    )
            );
        }
    }

    private String trackingUrl(Order order) {
        return publicUrl + "/track/" + order.getTrackingToken();
    }
}