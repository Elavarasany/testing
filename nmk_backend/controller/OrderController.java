package nmk_backend.controller;

import nmk_backend.dto.CreateOrderRequest;
import nmk_backend.dto.CreateCheckoutResponse;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;
import nmk_backend.service.OrderService;
import nmk_backend.service.RazorpayService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;

    public OrderController(
            OrderService orderService,
            OrderRepository orderRepository,
            RazorpayService razorpayService) {

        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.razorpayService = razorpayService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(
            @RequestBody CreateOrderRequest request) {

        Order order = null;

        try {
            // 1. Create pending order and reserve stock.
            order = orderService.createPendingOrder(request);

            // 2. Convert total amount into paise.
            int amountInPaise = order.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .intValueExact();

            // 3. Create Razorpay payment order.
            com.razorpay.Order razorpayOrder =
                    razorpayService.createOrder(amountInPaise);

            String razorpayOrderId = razorpayOrder.get("id");

            // 4. Save Razorpay order ID.
            order.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(order);

            // 5. Return checkout details to frontend.
            CreateCheckoutResponse response =
                    new CreateCheckoutResponse(
                            order.getId(),
                            razorpayOrderId,
                            amountInPaise,
                            "INR",
                            razorpayService.getKeyId()
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            releaseStockAfterFailure(order);

            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );

        } catch (Exception e) {

            e.printStackTrace();

            releaseStockAfterFailure(order);

            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Unable to create payment order")
            );
        }
    }

    private void releaseStockAfterFailure(Order order) {

        if (order == null) {
            return;
        }

        try {
            orderService.releaseReservation(order.getId());

        } catch (Exception e) {
            System.err.println(
                    "WARNING: Could not release stock reservation for order "
                            + order.getId() + ": " + e.getMessage()
            );
        }
    }
}