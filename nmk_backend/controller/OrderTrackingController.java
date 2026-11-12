
package nmk_backend.controller;

import nmk_backend.dto.OrderTrackingResponse;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/track")
public class OrderTrackingController {

    private final OrderRepository orderRepository;

    public OrderTrackingController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{trackingToken}")
    public ResponseEntity<?> trackOrder(
            @PathVariable String trackingToken) {

        return orderRepository.findByTrackingToken(trackingToken)
                .map(order -> {

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
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
