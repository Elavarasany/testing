
package nmk_backend.controller;
import nmk_backend.service.OrderService;
import nmk_backend.service.AdminActionAuthorizationService;
import org.springframework.security.core.Authentication;

import nmk_backend.dto.UpdateDeliveryDetailsRequest;
import nmk_backend.service.SmsService;
import nmk_backend.dto.AdminOrderItemResponse;
import nmk_backend.dto.AdminOrderResponse;
import nmk_backend.dto.UpdateOrderStatusRequest;
import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminActionAuthorizationService authorizationService;
    private final SmsService smsService;

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final String publicUrl;

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
            OrderService orderService,
            SmsService smsService,
            AdminActionAuthorizationService authorizationService,
            @Value("${app.public-url:http://localhost:5173}") String publicUrl
    ) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.smsService = smsService;
        this.authorizationService = authorizationService;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
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
                                    customer.put(
                                            "totalSpent",
                                            "PAID".equalsIgnoreCase(order.getPaymentStatus())
                                                    ? order.getTotalAmount()
                                                    : java.math.BigDecimal.ZERO
                                    );

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

                                    if (newAmount == null) {
                                        newAmount = java.math.BigDecimal.ZERO;
                                    }

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
    public ResponseEntity<?> clearAllOrders(
            @RequestHeader(
                    value = "X-Admin-Action-Authorization",
                    required = false
            ) String authorizationToken,
            Authentication authentication
    ) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(
                        Map.of("message", "Admin login required")
                );
            }

            authorizationService.consumeAuthorization(
                    authentication.getName(),
                    authorizationToken,
                    "CLEAR_ORDERS"
            );

            long deletedCount = orderRepository.count();

            orderRepository.deleteAll();

            return ResponseEntity.ok(Map.of(
                    "message", "All orders deleted successfully",
                    "deletedCount", deletedCount
            ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(403).body(
                    Map.of("message", exception.getMessage())
            );
        }
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

                    // Only unpaid pending orders can be cancelled through this endpoint.
// OrderService handles stock restoration safely.
                    if ("PENDING".equals(currentStatus)
                            && "CANCELLED".equals(newStatus)) {

                        try {
                            orderService.cancelPendingOrder(order.getId());

                            return ResponseEntity.ok(Map.of(
                                    "message", "Pending order cancelled and reserved stock released",
                                    "orderId", order.getId(),
                                    "orderStatus", "CANCELLED"
                            ));

                        } catch (IllegalArgumentException e) {
                            return ResponseEntity.notFound().build();

                        } catch (IllegalStateException e) {
                            return ResponseEntity.badRequest().body(
                                    Map.of("message", e.getMessage())
                            );
                        }
                    }

// Paid orders must not be cancelled without a refund process.
                    if ("CONFIRMED".equals(currentStatus)
                            && "CANCELLED".equals(newStatus)) {

                        return ResponseEntity.badRequest().body(
                                Map.of(
                                        "message",
                                        "Paid orders cannot be cancelled here. Refund handling is not implemented."
                                )
                        );
                    }

                    boolean validTransition = switch (currentStatus) {
                        case "CONFIRMED" ->
                                "SHIPPED".equals(newStatus);

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
                                    publicUrl + "/track/" + order.getTrackingToken()
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
