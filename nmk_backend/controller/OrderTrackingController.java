
package nmk_backend.controller;

import nmk_backend.dto.OrderTrackingResponse;
import nmk_backend.repository.OrderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/track")
public class OrderTrackingController {

    // Tracking tokens are 32 characters; anything longer cannot match an order
    private static final int MAX_TOKEN_LENGTH = 64;

    private final OrderRepository orderRepository;

    public OrderTrackingController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{trackingToken}")
    public ResponseEntity<?> trackOrder(
            @PathVariable String trackingToken) {

        if (trackingToken == null
                || trackingToken.isBlank()
                || trackingToken.length() > MAX_TOKEN_LENGTH) {
            return notFound();
        }

        return orderRepository.findByTrackingToken(trackingToken)
                .<ResponseEntity<?>>map(order -> {

                    List<OrderTrackingResponse.TrackingItemResponse> items =
                            order.getItems().stream()
                                    .map(item ->
                                            new OrderTrackingResponse.TrackingItemResponse(
                                                    item.getProductName(),
                                                    item.getQuantity()
                                            )
                                    )
                                    .toList();

                    OrderTrackingResponse response =
                            new OrderTrackingResponse(
                                    order.getOrderStatus(),
                                    order.getTotalAmount(),
                                    order.getOrderDate(),
                                    order.getCourierName(),
                                    order.getTrackingNumber(),
                                    order.getCustomerName(),
                                    order.getCity(),
                                    items
                            );

                    return ResponseEntity.ok(response);
                })
                .orElseGet(this::notFound);
    }

    private ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("message", "Order not found. Check your tracking link and try again.")
        );
    }
}
