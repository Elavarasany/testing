package nmk_backend.controller;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import nmk_backend.dto.AdminOrderItemResponse;
import nmk_backend.dto.AdminOrderResponse;
import nmk_backend.dto.UpdateOrderStatusRequest;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderRepository orderRepository;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING",
            "CONFIRMED",
            "SHIPPED",
            "DELIVERED",
            "CANCELLED"
    );

    public AdminOrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Get all customer orders
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminOrderResponse>> getAllOrders() {

        List<AdminOrderResponse> orders = orderRepository
                .findAllWithItems()
                .stream()
                .map(this::convertToResponse)
                .toList();

        return ResponseEntity.ok(orders);
    }

    // Get unique customers from existing orders
    @GetMapping("/customers")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCustomers() {

        List<java.util.Map<String, Object>> customers =
                orderRepository.findAllWithItems()
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                order -> order.getEmail() != null
                                        ? order.getEmail().trim().toLowerCase()
                                        : String.valueOf(order.getPhone()),
                                order -> {
                                    java.util.Map<String, Object> customer =
                                            new java.util.LinkedHashMap<>();

                                    customer.put("customerName", order.getCustomerName());
                                    customer.put("email", order.getEmail());
                                    customer.put("phone", order.getPhone());
                                    customer.put("totalOrders", 1);
                                    customer.put("totalSpent", order.getTotalAmount());

                                    return customer;
                                },
                                (existing, current) -> {
                                    existing.put(
                                            "totalOrders",
                                            (int) existing.get("totalOrders") + 1
                                    );

                                    java.math.BigDecimal oldAmount =
                                            (java.math.BigDecimal) existing.get("totalSpent");

                                    java.math.BigDecimal newAmount =
                                            (java.math.BigDecimal) current.get("totalSpent");

                                    existing.put(
                                            "totalSpent",
                                            oldAmount.add(newAmount != null
                                                    ? newAmount
                                                    : java.math.BigDecimal.ZERO)
                                    );

                                    return existing;
                                },
                                java.util.LinkedHashMap::new
                        ))
                        .values()
                        .stream()
                        .toList();

        return ResponseEntity.ok(customers);
    }

    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<?> clearAllOrders() {

        long deletedCount = orderRepository.count();

        orderRepository.deleteAll();

        return ResponseEntity.ok(Map.of(
                "message", "All orders deleted successfully",
                "deletedCount", deletedCount
        ));
    }

    // Update delivery status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request) {

        if (request.status() == null) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Status is required"));
        }

        String status = request.status().trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(status)) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Invalid order status"));
        }

        return orderRepository.findById(id)
                .map(order -> {
                    order.setOrderStatus(status);
                    orderRepository.save(order);

                    return ResponseEntity.ok(
                            java.util.Map.of(
                                    "message", "Order status updated successfully",
                                    "orderId", order.getId(),
                                    "orderStatus", order.getOrderStatus()
                            )
                    );
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Convert entity into safe response DTO
    private AdminOrderResponse convertToResponse(Order order) {

        List<AdminOrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new AdminOrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();

        return new AdminOrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getEmail(),
                order.getPhone(),
                order.getAddress(),
                order.getCity(),
                order.getState(),
                order.getPincode(),
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getRazorpayOrderId(),
                order.getRazorpayPaymentId(),
                order.getOrderDate(),
                items
        );
    }
}