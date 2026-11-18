package nmk_backend.service;

import nmk_backend.entity.Order;
import nmk_backend.exception.PaymentConfirmationConflictException;
import nmk_backend.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

@Service
public class PaymentVerificationService {

    private final OrderRepository orderRepository;

    public PaymentVerificationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationResult confirmPayment(
            String razorpayOrderId,
            String razorpayPaymentId) {

        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay order ID and payment ID are required."
            );
        }

        Order order = orderRepository
                .findByRazorpayOrderIdForUpdate(razorpayOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            String recordedPaymentId = order.getRazorpayPaymentId();

            if (recordedPaymentId == null || recordedPaymentId.isBlank()) {
                throw new PaymentConfirmationConflictException(
                        "This order is marked PAID but has no recorded payment ID. " +
                                "Payment requires manual reconciliation."
                );
            }

            if (!recordedPaymentId.equals(razorpayPaymentId)) {
                throw new PaymentConfirmationConflictException(
                        "A different payment is already recorded for this order. " +
                                "The additional captured payment requires refund " +
                                "and manual reconciliation."
                );
            }

            return new ConfirmationResult(order, false);
        }

        if (!order.isStockReserved()) {
            throw new PaymentConfirmationConflictException(
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

        order.setStockReserved(false);

        Order savedOrder = orderRepository.save(order);

        return new ConfirmationResult(savedOrder, true);
    }

    public record ConfirmationResult(
            Order order,
            boolean newlyConfirmed
    ) {}
}
