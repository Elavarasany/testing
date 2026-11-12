
package nmk_backend.controller;
import nmk_backend.service.SmsService;
import nmk_backend.dto.VerifyPaymentRequest;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;
import nmk_backend.service.RazorpayService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class RazorpayController {

    private final RazorpayService razorpayService;
    private final OrderRepository orderRepository;
    private final SmsService smsService;

    public RazorpayController(
            RazorpayService razorpayService,
            OrderRepository orderRepository,
            SmsService smsService) {

        this.razorpayService = razorpayService;
        this.orderRepository = orderRepository;
        this.smsService = smsService;
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

            // 3. Prevent duplicate payment processing
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                return ResponseEntity.ok(
                        Map.of(
                                "verified", true,
                                "message", "Order already paid",
                                "databaseOrderId", order.getId()
                        )
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
            String paymentId = request.getRazorpayPaymentId();

            order.setRazorpayPaymentId(paymentId);
            order.setPaymentStatus("PAID");

            if ("PENDING".equalsIgnoreCase(order.getOrderStatus())) {
                order.setOrderStatus("CONFIRMED");
            }

            orderRepository.save(order);
            smsService.sendOrderNotification(
                    order.getPhone(),
                    String.valueOf(order.getId()),
                    "CONFIRMED",
                    "",
                    "",
                    "http://localhost:5173/track/" + order.getTrackingToken()
            );

            // 7. Return successful verification response
            return ResponseEntity.ok(
                    Map.of(
                            "verified", true,
                            "message", "Payment verified and order saved successfully",
                            "databaseOrderId", order.getId()
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
}
