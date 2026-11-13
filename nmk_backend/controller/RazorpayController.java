
package nmk_backend.controller;

import nmk_backend.dto.VerifyPaymentRequest;
import nmk_backend.entity.Order;
import nmk_backend.entity.OrderItem;
import nmk_backend.repository.OrderRepository;
import nmk_backend.repository.ProductRepository;
import nmk_backend.service.RazorpayService;
import nmk_backend.service.SmsService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class RazorpayController {

    private final RazorpayService razorpayService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SmsService smsService;
    private final String publicUrl;

    public RazorpayController(
            RazorpayService razorpayService,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            SmsService smsService,
            @Value("${app.public-url:http://localhost:5173}") String publicUrl) {

        this.razorpayService = razorpayService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.smsService = smsService;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody VerifyPaymentRequest request) {

        try {

            // 1. Verify Razorpay signature
            boolean verified = razorpayService.verifySignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );

            if (!verified) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "verified", false,
                                "message", "Payment signature verification failed"
                        )
                );
            }

            // 2. Find corresponding MySQL order
            Order order = orderRepository
                    .findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Order not found")
                    );

            // 3. Prevent duplicate payment processing. Stock is reduced only on the first verification.
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                return ResponseEntity.ok(
                        verifiedResponse(order, "Order already paid")
                );
            }

            // 4. Calculate expected amount in paise
            int expectedAmount = order.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .intValueExact();

            // 5. Confirm payment is captured by Razorpay
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

            // 6. Update MySQL order
            order.setRazorpayPaymentId(request.getRazorpayPaymentId());
            order.setPaymentStatus("PAID");

            if ("PENDING".equalsIgnoreCase(order.getOrderStatus())) {
                order.setOrderStatus("CONFIRMED");
            }

            ensureTrackingToken(order);
            reduceStock(order);
            orderRepository.save(order);

            try {
                smsService.sendOrderNotification(
                        order.getPhone(),
                        String.valueOf(order.getId()),
                        "CONFIRMED",
                        "",
                        "",
                        trackingUrl(order)
                );
            } catch (Exception e) {
                System.err.println(
                        "Payment successful, but confirmation SMS failed for order "
                                + order.getId() + ": " + e.getMessage()
                );
            }

            // 7. Return the verification result, including the customer's tracking link
            return ResponseEntity.ok(
                    verifiedResponse(order, "Payment verified and order saved successfully")
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

    // Shared response for the first verification and for repeat calls on a paid order
    private Map<String, Object> verifiedResponse(Order order, String message) {
        ensureTrackingToken(order);
        orderRepository.save(order);

        return Map.of(
                "verified", true,
                "message", message,
                "databaseOrderId", order.getId(),
                "trackingToken", order.getTrackingToken(),
                "trackingUrl", trackingUrl(order)
        );
    }

    // Orders created before tracking tokens existed get one the first time they are needed
    private void ensureTrackingToken(Order order) {
        if (order.getTrackingToken() == null || order.getTrackingToken().isBlank()) {
            order.setTrackingToken(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private String trackingUrl(Order order) {
        return publicUrl + "/track/" + order.getTrackingToken();
    }

    // Takes the purchased quantities off product stock. Stock never drops below zero.
    private void reduceStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int current = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

                product.setStockQuantity(Math.max(0, current - quantity));
                productRepository.save(product);
            });
        }
    }
}
