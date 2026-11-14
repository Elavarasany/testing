package nmk_backend.service;

import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentVerificationService {

    private final OrderRepository orderRepository;

    public PaymentVerificationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ConfirmationResult confirmPayment(
            String razorpayOrderId,
            String razorpayPaymentId) {

        // Lock this order so concurrent verification requests
        // cannot confirm it at the same time.
        Order order = orderRepository
                .findByRazorpayOrderIdForUpdate(razorpayOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );

        // Idempotency: never process an already-paid order again.
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return new ConfirmationResult(order, false);
        }

        // Checkout must have reserved stock before payment confirmation.
        if (!order.isStockReserved()) {
            throw new IllegalStateException(
                    "Stock reservation is no longer active. " +
                            "Payment requires manual review."
            );
        }

        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setPaymentStatus("PAID");

        if ("PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            order.setOrderStatus("CONFIRMED");
        }

        if (order.getTrackingToken() == null ||
                order.getTrackingToken().isBlank()) {
            order.setTrackingToken(
                    UUID.randomUUID().toString().replace("-", "")
            );
        }

        // Stock was already deducted at checkout.
        // Mark the reservation as consumed; DO NOT deduct stock again.
        order.setStockReserved(false);

        Order savedOrder = orderRepository.save(order);

        return new ConfirmationResult(savedOrder, true);
    }

    public record ConfirmationResult(
            Order order,
            boolean newlyConfirmed
    ) {}
}