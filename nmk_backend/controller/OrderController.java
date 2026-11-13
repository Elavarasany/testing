
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
@CrossOrigin(origins = "http://localhost:5173")
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

        try {

            // 1. Create pending MySQL order
            Order order = orderService.createPendingOrder(request);

            // 2. Convert INR to paise
            int amountInPaise = order.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .intValueExact();

            // 3. Create Razorpay order
            com.razorpay.Order razorpayOrder =
                    razorpayService.createOrder(amountInPaise);

            String razorpayOrderId = razorpayOrder.get("id");

            // 4. Link Razorpay order with MySQL order
            order.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(order);

            // 5. Return checkout details
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

            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Unable to create payment order")
            );
        }
    }
}
