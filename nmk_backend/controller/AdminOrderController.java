
package nmk_backend.controller;

import nmk_backend.dto.UpdateDeliveryDetailsRequest;
import nmk_backend.service.SmsService;
import nmk_backend.dto.AdminOrderItemResponse;
import nmk_backend.dto.AdminOrderResponse;
import nmk_backend.dto.UpdateOrderStatusRequest;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final SmsService smsService;

    private final OrderRepository orderRepository;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING",
            "CONFIRMED",
            "SHIPPED",
            "OUT_FOR_DELIVERY",
            "DELIVERED",
            "CANCELLED"
    );

    public AdminOrderController(
            OrderRepository orderRepository,
            SmsService smsService) {

        this.orderRepository = orderRepository;
        this.smsService = smsService;
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

        List<Map<String, Object>> customers =
                orderRepository.findAllWithItems()
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                order -> order.getEmail() != null
                                        ? order.getEmail().trim().toLowerCase()
                                        : String.valueOf(order.getPhone()),
                                order -> {
                                    Map<String, Object> customer =
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


    // Update order status with payment and transition validation
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request) {

        if (request.status() == null || request.status().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Status is required"));
        }

        String newStatus = request.status().trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(newStatus)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid order status"));
        }

        return orderRepository.findById(id)
                .map(order -> {

                    String currentStatus = order.getOrderStatus();

                    // Only Razorpay verification can confirm payment
                    if ("CONFIRMED".equals(newStatus)) {
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Orders can only be confirmed through verified payment"
                                ));
                    }

                    // Delivery cannot proceed without captured payment
                    if (Set.of(
                            "SHIPPED",
                            "OUT_FOR_DELIVERY",
                            "DELIVERED"
                    ).contains(newStatus)
                            && !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {

                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Payment must be verified before delivery"
                                ));
                    }

                    boolean validTransition = switch (currentStatus) {
                        case "PENDING" ->
                                "CANCELLED".equals(newStatus);

                        case "CONFIRMED" ->
                                Set.of("SHIPPED", "CANCELLED").contains(newStatus);

                        case "SHIPPED" ->
                                "OUT_FOR_DELIVERY".equals(newStatus);

                        case "OUT_FOR_DELIVERY" ->
                                "DELIVERED".equals(newStatus);

                        default -> false;
                    };

                    if (!validTransition) {
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Invalid status transition: "
                                                + currentStatus + " to " + newStatus
                                ));
                    }

                    if ("SHIPPED".equals(newStatus)
                            && (order.getCourierName() == null
                            || order.getCourierName().isBlank()
                            || order.getTrackingNumber() == null
                            || order.getTrackingNumber().isBlank())) {

                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Add courier name and tracking number before shipping"
                                ));
                    }

                    order.setOrderStatus(newStatus);
                    // Generate tracking token for older orders
                    if (order.getTrackingToken() == null ||
                            order.getTrackingToken().isBlank()) {

                        order.setTrackingToken(
                                java.util.UUID.randomUUID()
                                        .toString()
                                        .replace("-", "")
                        );
                    }
                    orderRepository.save(order);
                    if ("SHIPPED".equals(newStatus)
                            || "OUT_FOR_DELIVERY".equals(newStatus)
                            || "DELIVERED".equals(newStatus)) {

                        try {
                            smsService.sendOrderNotification(
                                    order.getPhone(),
                                    String.valueOf(order.getId()),
                                    newStatus,
                                    order.getCourierName() == null ? "" : order.getCourierName(),
                                    order.getTrackingNumber() == null ? "" : order.getTrackingNumber(),
                                    "http://localhost:5173/track/" + order.getTrackingToken()
                            );
                        } catch (Exception e) {
                            System.err.println("SMS notification failed for order "
                                    + order.getId() + ": " + e.getMessage());
                        }
                    }

                    return ResponseEntity.ok(Map.of(
                            "message", "Order status updated successfully",
                            "orderId", order.getId(),
                            "orderStatus", order.getOrderStatus()
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // Update courier and tracking information
    @PutMapping("/{id}/delivery")
    public ResponseEntity<?> updateDeliveryDetails(
            @PathVariable Long id,
            @RequestBody UpdateDeliveryDetailsRequest request) {

        if (request.courierName() == null
                || request.courierName().isBlank()
                || request.trackingNumber() == null
                || request.trackingNumber().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Courier name and tracking number are required"
                    ));
        }

        return orderRepository.findById(id)
                .map(order -> {

                    if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Delivery details cannot be added before payment confirmation"
                                ));
                    }

                    if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())
                            || "DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Delivery details cannot be changed for this order"
                                ));
                    }

                    order.setCourierName(request.courierName().trim());
                    order.setTrackingNumber(request.trackingNumber().trim());

                    orderRepository.save(order);

                    return ResponseEntity.ok(Map.of(
                            "message", "Delivery details updated successfully",
                            "orderId", order.getId(),
                            "courierName", order.getCourierName(),
                            "trackingNumber", order.getTrackingNumber()
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // Convert entity into response DTO
    private AdminOrderResponse convertToResponse(Order order) {

        List<AdminOrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new AdminOrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getPrescriptionType(),
                        item.getLensType(),
                        item.getPhotochromic()
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
                order.getCourierName(),
                order.getTrackingNumber(),
                order.getTrackingToken(),
                order.getOrderDate(),
                items
        );
    }
}
