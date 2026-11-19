package nmk_backend.controller;

import nmk_backend.service.RazorpayService;
import nmk_backend.service.RazorpayWebhookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payment")
public class RazorpayWebhookController {

    private final RazorpayService razorpayService;
    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(
            RazorpayService razorpayService,
            RazorpayWebhookService webhookService) {
        this.razorpayService = razorpayService;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false)
            String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false)
            String eventId,
            @RequestBody(required = false) byte[] rawBody) {

        if (signature == null || signature.isBlank()
                || eventId == null || eventId.isBlank()
                || rawBody == null || rawBody.length == 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("received", false, "message", "Missing webhook headers or body")
            );
        }

        if (rawBody.length > 1_048_576) {
            return ResponseEntity.status(413).body(
                    Map.of("received", false, "message", "Webhook body exceeds the size limit")
            );
        }

        try {
            if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
                return ResponseEntity.status(401).body(
                        Map.of("received", false, "message", "Invalid webhook signature")
                );
            }

            webhookService.storeEvent(
                    eventId.trim(),
                    new String(rawBody, StandardCharsets.UTF_8)
            );
            return ResponseEntity.ok(
                    Map.of("received", true, "message", "Webhook event stored")
            );

        } catch (DataIntegrityViolationException duplicate) {
            // The unique event_id constraint is the final concurrency-safe
            // deduplication guard. A duplicate delivery is already durable.
            return ResponseEntity.ok(
                    Map.of("received", true, "duplicate", true)
            );
        } catch (IllegalArgumentException invalidPayload) {
            return ResponseEntity.badRequest().body(
                    Map.of("received", false, "message", invalidPayload.getMessage())
            );
        } catch (Exception error) {
            // Return non-2xx so Razorpay can retry if durable storage failed.
            return ResponseEntity.internalServerError().body(
                    Map.of("received", false, "message", "Webhook could not be stored")
            );
        }
    }
}
