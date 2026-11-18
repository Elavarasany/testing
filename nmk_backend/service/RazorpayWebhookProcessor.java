
package nmk_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import nmk_backend.exception.PaymentConfirmationConflictException;

import nmk_backend.entity.Order;
import nmk_backend.entity.RazorpayWebhookEvent;
import nmk_backend.repository.OrderRepository;
import nmk_backend.repository.RazorpayWebhookEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RazorpayWebhookProcessor {

    private static final int MAX_ATTEMPTS = 8;

    // A worker can own an event for five minutes.
    // A later worker may reclaim it if the original worker disappears.
    private static final long LEASE_MINUTES = 5;

    private final RazorpayWebhookEventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final PaymentVerificationService paymentVerificationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public RazorpayWebhookProcessor(
            RazorpayWebhookEventRepository eventRepository,
            OrderRepository orderRepository,
            RazorpayService razorpayService,
            PaymentVerificationService paymentVerificationService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {

        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.razorpayService = razorpayService;
        this.paymentVerificationService = paymentVerificationService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Claims an event in a short transaction, processes it outside
     * the transaction, and finalizes it using the claim token.
     */
    public void processOne(Long eventId) throws Exception {

        Claim claim = claimEvent(eventId);

        if (claim == null) {
            return;
        }

        ProcessingResult result;

        try {
            processPayload(claim.rawPayload());

            result = ProcessingResult.processed();

        } catch (ManualReviewException permanent) {

            result = ProcessingResult.manualReview(
                    safeMessage(permanent)
            );

        } catch (Exception retryable) {

            if (claim.attemptCount() >= MAX_ATTEMPTS) {

                result = ProcessingResult.manualReview(
                        "Retry limit reached: "
                                + retryable.getClass().getSimpleName()
                );

            } else {

                long delaySeconds = Math.min(
                        3600L,
                        15L << Math.min(claim.attemptCount() - 1, 8)
                );

                result = ProcessingResult.retry(
                        "Retryable processing error: "
                                + retryable.getClass().getSimpleName(),
                        delaySeconds
                );
            }
        }

        finalizeEvent(eventId, claim.claimToken(), result);
    }

    /**
     * Claims an event and commits the claim before any external API call.
     */
    private Claim claimEvent(Long eventId) {

        return transactionTemplate.execute(transactionStatus -> {

            RazorpayWebhookEvent event =
                    eventRepository.findByIdForUpdate(eventId)
                            .orElse(null);

            if (event == null) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();

            boolean ready =
                    "RECEIVED".equals(event.getStatus())
                            || "RETRY".equals(event.getStatus());

            boolean staleProcessing =
                    "PROCESSING".equals(event.getStatus())
                            && (
                            event.getProcessingStartedAt() == null
                                    || !event.getProcessingStartedAt().isAfter(
                                    now.minusMinutes(LEASE_MINUTES)
                            )
                    );

            if (!ready && !staleProcessing) {
                return null;
            }

            if (ready
                    && event.getNextAttemptAt() != null
                    && event.getNextAttemptAt().isAfter(now)) {
                return null;
            }

            String claimToken = UUID.randomUUID().toString();

            event.setStatus("PROCESSING");
            event.setProcessingStartedAt(now);
            event.setClaimToken(claimToken);
            event.setAttemptCount(event.getAttemptCount() + 1);
            event.setProcessingError(null);
            event.setNextAttemptAt(null);

            eventRepository.save(event);

            return new Claim(
                    event.getRawPayload(),
                    claimToken,
                    event.getAttemptCount()
            );
        });
    }

    /**
     * Runs outside the claim transaction.
     * Only a verified payment.captured event can confirm an order.
     */
    private void processPayload(String rawPayload) throws Exception {

        JsonNode root = objectMapper.readTree(rawPayload);

        String eventType = root.path("event").asText("");

        // Other signed webhook events are recorded but do not
        // change order payment status.
        if (!"payment.captured".equals(eventType)) {
            return;
        }

        JsonNode payment = root.path("payload")
                .path("payment")
                .path("entity");

        String paymentId = payment.path("id").asText("");
        String razorpayOrderId = payment.path("order_id").asText("");

        if (paymentId.isBlank()
                || razorpayOrderId.isBlank()
                || !payment.path("amount").canConvertToInt()) {

            throw new ManualReviewException(
                    "Captured payment payload is incomplete"
            );
        }

        Order order = orderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() ->
                        new ManualReviewException(
                                "No local order matches the captured payment"
                        )
                );

        int expectedPaise;

        try {
            expectedPaise = order.getTotalAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .intValueExact();

        } catch (Exception invalidAmount) {

            throw new ManualReviewException(
                    "Local order amount cannot be verified"
            );
        }

        if (payment.path("amount").asInt() != expectedPaise) {

            throw new ManualReviewException(
                    "Captured payment amount does not match the order"
            );
        }

        // External Razorpay API call happens without holding
        // the webhook database row lock or claim transaction.
        boolean captured =
                razorpayService.isPaymentCapturedForOrder(
                        paymentId,
                        razorpayOrderId,
                        expectedPaise
                );

        if (!captured) {
            throw new IllegalStateException(
                    "Razorpay has not yet confirmed the captured payment"
            );
        }

        // This service uses its own transaction and order row lock.
        // It must remain the authority for payment confirmation.
        try {

            paymentVerificationService.confirmPayment(
                    razorpayOrderId,
                    paymentId
            );

        } catch (PaymentConfirmationConflictException
                 | IllegalStateException
                 | IllegalArgumentException conflict) {

            throw new ManualReviewException(
                    safeMessage(conflict)
            );
        }
    }

    /**
     * Finalizes only if this worker still owns the event.
     * A worker whose lease was reclaimed cannot overwrite
     * the newer worker's result.
     */
    private void finalizeEvent(
            Long eventId,
            String claimToken,
            ProcessingResult result) {

        transactionTemplate.executeWithoutResult(transactionStatus -> {

            RazorpayWebhookEvent event =
                    eventRepository.findByIdForUpdate(eventId)
                            .orElse(null);

            if (event == null) {
                return;
            }

            if (!"PROCESSING".equals(event.getStatus())
                    || !claimToken.equals(event.getClaimToken())) {

                // Ownership changed. This worker is stale.
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            if (result.status().equals("PROCESSED")) {

                event.setStatus("PROCESSED");
                event.setProcessedAt(now);
                event.setNextAttemptAt(null);
                event.setProcessingError(null);

            } else if (result.status().equals("MANUAL_REVIEW")) {

                event.setStatus("MANUAL_REVIEW");
                event.setProcessingError(
                        safeMessage(result.error())
                );
                event.setNextAttemptAt(null);

            } else {

                event.setStatus("RETRY");
                event.setProcessingError(
                        safeMessage(result.error())
                );
                event.setNextAttemptAt(
                        now.plusSeconds(result.delaySeconds())
                );
            }

            // Release the worker's ownership after finalization.
            event.setClaimToken(null);
            event.setProcessingStartedAt(null);

            eventRepository.save(event);
        });
    }

    private String safeMessage(Exception error) {

        String message = error.getMessage();

        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }

        return message.length() > 900
                ? message.substring(0, 900)
                : message;
    }

    private String safeMessage(String message) {

        if (message == null || message.isBlank()) {
            return "Processing failed";
        }

        return message.length() > 900
                ? message.substring(0, 900)
                : message;
    }

    private record Claim(
            String rawPayload,
            String claimToken,
            int attemptCount
    ) {
    }

    private record ProcessingResult(
            String status,
            String error,
            long delaySeconds
    ) {

        static ProcessingResult processed() {
            return new ProcessingResult("PROCESSED", null, 0);
        }

        static ProcessingResult manualReview(String error) {
            return new ProcessingResult("MANUAL_REVIEW", error, 0);
        }

        static ProcessingResult retry(String error, long delaySeconds) {
            return new ProcessingResult("RETRY", error, delaySeconds);
        }
    }

    private static class ManualReviewException extends Exception {

        private ManualReviewException(String message) {
            super(message);
        }
    }
}
