package nmk_backend.service;

import nmk_backend.entity.Order;
import nmk_backend.repository.OrderRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockReservationScheduler {

    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;

    public StockReservationScheduler(
            OrderRepository orderRepository,
            RazorpayService razorpayService,
            OrderService orderService,
            PlatformTransactionManager transactionManager) {

        this.orderRepository = orderRepository;
        this.razorpayService = razorpayService;
        this.orderService = orderService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // Run every minute.
    @Scheduled(fixedDelay = 60000)
    public void expireReservations() {

        List<Long> expiredOrderIds =
                orderRepository.findExpiredReservationIds(
                        LocalDateTime.now()
                );

        for (Long orderId : expiredOrderIds) {
            processExpiredReservation(orderId);
        }
    }

    private void processExpiredReservation(Long orderId) {

        try {
            // Read the order's Razorpay ID without holding a database lock
            // during the external API request.
            Order snapshot = orderRepository.findById(orderId)
                    .orElse(null);

            if (snapshot == null ||
                    snapshot.getRazorpayOrderId() == null ||
                    snapshot.getRazorpayOrderId().isBlank()) {
                System.err.println(
                        "Reservation requires review. Missing Razorpay order ID: "
                                + orderId
                );
                return;
            }

            // Check Razorpay before releasing stock.
            // If Razorpay cannot be reached, retain the reservation.
            boolean paymentNeedsRetention =
                    razorpayService.hasUnsettledOrSuccessfulPayment(
                            snapshot.getRazorpayOrderId()
                    );

            if (paymentNeedsRetention) {
                System.out.println(
                        "Reservation retained for payment review. Order: "
                                + orderId
                );
                return;
            }

            // Recheck expiry and lock the order before releasing stock.
            transactionTemplate.executeWithoutResult(status -> {

                Order lockedOrder = orderRepository
                        .findByIdForUpdate(orderId)
                        .orElse(null);

                if (lockedOrder == null) {
                    return;
                }

                if (!lockedOrder.isStockReserved()
                        || !"PENDING".equalsIgnoreCase(
                        lockedOrder.getPaymentStatus())
                        || lockedOrder.getReservationExpiresAt() == null
                        || lockedOrder.getReservationExpiresAt()
                        .isAfter(LocalDateTime.now())) {
                    return;
                }

                // Use the existing locked release method.
                orderService.releaseReservation(orderId);
            });

        } catch (Exception e) {
            // Fail closed: do not release stock when payment status
            // cannot be established safely.
            System.err.println(
                    "Could not process reservation expiry for order "
                            + orderId + ": " + e.getMessage()
            );
        }
    }
}