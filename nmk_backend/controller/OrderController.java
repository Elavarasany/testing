
package nmk_backend.controller;

import nmk_backend.dto.CreateOrderRequest;
import nmk_backend.dto.CreateCheckoutResponse;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;
import nmk_backend.service.OrderService;
import nmk_backend.service.RazorpayService;

import org.springframework.http.HttpStatus;
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

        // PHASE 1:
        // Verify Razorpay credentials before reserving stock.
        try {
            razorpayService.requirePaymentCredentials();

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Online payment is temporarily unavailable"
                    ));
        }

        Order order = null;
        int amountInPaise;

        // PHASE 2:
        // Create pending order and reserve stock.
        // Razorpay has not been called yet, so failures are safe to release.
        try {
            order = orderService.createPendingOrder(request);

            amountInPaise = order.getTotalAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .intValueExact();

            if (amountInPaise <= 0) {
                throw new IllegalArgumentException(
                        "Invalid payment amount"
                );
            }

        } catch (IllegalArgumentException e) {
            releaseStockAfterFailure(order);

            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );

        } catch (Exception e) {
            System.err.println(
                    "Checkout preparation failed: " + e.getMessage()
            );

            releaseStockAfterFailure(order);

            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Unable to prepare checkout")
            );
        }

        // PHASE 3:
        // Persist IN_PROGRESS BEFORE making the external Razorpay request.
        // If this fails, Razorpay has not been called, so stock can be released.
        try {
            order.setPaymentCreationStatus("IN_PROGRESS");
            orderRepository.saveAndFlush(order);

        } catch (Exception e) {
            System.err.println(
                    "Could not persist payment creation status for order "
                            + order.getId() + ": " + e.getMessage()
            );

            releaseStockAfterFailure(order);

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "message",
                            "Unable to prepare payment. Please try again."
                    ));
        }

        // PHASE 4:
        // Razorpay request begins here.
        //
        // IMPORTANT:
        // From this point onward, an exception may mean Razorpay
        // created an order even if our application did not receive
        // or save its response.
        //
        // NEVER release stock automatically after this point.
        // IN_PROGRESS remains available for manual reconciliation.
        try {
            com.razorpay.Order razorpayOrder =
                    razorpayService.createOrder(amountInPaise);

            String razorpayOrderId = razorpayOrder.get("id");

            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                throw new IllegalStateException(
                        "Razorpay returned an invalid order ID"
                );
            }

            // Save provider order ID and mark creation as successful.
            order.setRazorpayOrderId(razorpayOrderId);
            order.setPaymentCreationStatus("CREATED");

            orderRepository.saveAndFlush(order);

            CreateCheckoutResponse response =
                    new CreateCheckoutResponse(
                            order.getId(),
                            razorpayOrderId,
                            amountInPaise,
                            "INR",
                            razorpayService.getKeyId()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(
                    "Razorpay checkout result requires review. Order ID: "
                            + order.getId()
                            + ". Reason: " + e.getMessage()
            );

            // Intentionally retain stock reservation.
            // Payment creation status remains IN_PROGRESS if the
            // final database save did not complete.
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "message",
                            "Payment setup could not be confirmed. "
                                    + "Your order is retained for payment review. "
                                    + "Please contact support before retrying."
                    ));
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
