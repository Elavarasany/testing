
package nmk_backend.service;

import nmk_backend.entity.Order;
import nmk_backend.entity.OrderItem;
import nmk_backend.exception.PaymentConfirmationConflictException;
import nmk_backend.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class PaymentVerificationService {

    private static final String VARIANT_INVENTORY = "VARIANT_INVENTORY";
    private static final String ORDER_REFERENCE = "ORDER";

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public PaymentVerificationService(
            OrderRepository orderRepository,
            InventoryService inventoryService
    ) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationResult confirmPayment(
            String razorpayOrderId,
            String razorpayPaymentId
    ) {

        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay order ID and payment ID are required."
            );
        }

        Order order = orderRepository
                .findByRazorpayOrderIdForUpdate(razorpayOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found.")
                );

        // Idempotent handling of an already confirmed payment.
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {

            String recordedPaymentId = order.getRazorpayPaymentId();

            if (recordedPaymentId == null || recordedPaymentId.isBlank()) {
                throw new PaymentConfirmationConflictException(
                        "This order is marked PAID but has no recorded payment ID. "
                                + "Payment requires manual reconciliation."
                );
            }

            if (!recordedPaymentId.equals(razorpayPaymentId)) {
                throw new PaymentConfirmationConflictException(
                        "A different payment is already recorded for this order. "
                                + "The additional captured payment requires refund "
                                + "and manual reconciliation."
                );
            }

            return new ConfirmationResult(order, false);
        }

        if (!order.isStockReserved()) {
            throw new PaymentConfirmationConflictException(
                    "Stock reservation is no longer active. "
                            + "Payment requires manual review."
            );
        }

        /*
         * New variant-based orders consume the reservation.
         * Legacy orders already deducted Product.stockQuantity when
         * their reservation was created, so they must not consume again.
         */
        String reservationType = order.getReservationType();

        if (VARIANT_INVENTORY.equalsIgnoreCase(
                reservationType == null ? "" : reservationType
        )) {
            consumeVariantReservation(order);

        } else if (reservationType == null
                || reservationType.isBlank()
                || "LEGACY_PRODUCT".equalsIgnoreCase(reservationType)) {
            // Legacy reservation: no InventoryService consumption.
        } else {
            throw new PaymentConfirmationConflictException(
                    "Unknown stock reservation type. "
                            + "Payment requires manual reconciliation."
            );
        }

        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setPaymentStatus("PAID");

        if ("PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            order.setOrderStatus("CONFIRMED");
        }

        if (order.getTrackingToken() == null
                || order.getTrackingToken().isBlank()) {
            order.setTrackingToken(
                    UUID.randomUUID().toString().replace("-", "")
            );
        }

        order.setStockReserved(false);
        order.setReservationExpiresAt(null);

        Order savedOrder = orderRepository.save(order);

        return new ConfirmationResult(savedOrder, true);
    }

    private void consumeVariantReservation(Order order) {

        if (order.getInventoryLocationId() == null) {
            throw new PaymentConfirmationConflictException(
                    "Variant inventory location is missing. "
                            + "Payment requires manual reconciliation."
            );
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new PaymentConfirmationConflictException(
                    "Order has no items to consume from inventory. "
                            + "Payment requires manual reconciliation."
            );
        }

        Map<Long, Integer> quantities = new TreeMap<>();

        for (OrderItem item : order.getItems()) {

            if (item.getVariantId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new PaymentConfirmationConflictException(
                        "Order contains incomplete variant reservation details. "
                                + "Payment requires manual reconciliation."
                );
            }

            quantities.merge(
                    item.getVariantId(),
                    item.getQuantity(),
                    (current, added) -> {
                        try {
                            return Math.addExact(current, added);
                        } catch (ArithmeticException exception) {
                            throw new PaymentConfirmationConflictException(
                                    "Order quantity exceeds the supported limit. "
                                            + "Payment requires manual reconciliation."
                            );
                        }
                    }
            );
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {

            Long variantId = entry.getKey();
            int quantity = entry.getValue();

            String idempotencyKey =
                    "ORDER-" + order.getId()
                            + "-VARIANT-" + variantId
                            + "-CONSUME";

            inventoryService.consumeReservation(
                    order.getInventoryLocationId(),
                    variantId,
                    quantity,
                    idempotencyKey,
                    null,
                    "Customer payment confirmed",
                    ORDER_REFERENCE,
                    order.getId()
            );
        }
    }

    public record ConfirmationResult(
            Order order,
            boolean newlyConfirmed
    ) {
    }
}
