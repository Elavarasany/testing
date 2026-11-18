
package nmk_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import nmk_backend.entity.RazorpayWebhookEvent;
import nmk_backend.repository.RazorpayWebhookEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RazorpayWebhookService {

    private static final Logger log =
            LoggerFactory.getLogger(RazorpayWebhookService.class);

    private static final long LEASE_MINUTES = 5;
    private static final int BATCH_SIZE = 25;

    private final RazorpayWebhookEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final RazorpayWebhookProcessor processor;

    public RazorpayWebhookService(
            RazorpayWebhookEventRepository eventRepository,
            ObjectMapper objectMapper,
            RazorpayWebhookProcessor processor) {

        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @Transactional
    public void storeEvent(String eventId, String rawPayload) {

        if (eventId == null
                || eventId.isBlank()
                || eventId.length() > 150) {

            throw new IllegalArgumentException(
                    "Invalid Razorpay event ID"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            String eventType = root.path("event").asText("");

            if (eventType.isBlank() || eventType.length() > 100) {
                throw new IllegalArgumentException(
                        "Webhook event type is missing or invalid"
                );
            }

            RazorpayWebhookEvent event =
                    new RazorpayWebhookEvent();

            event.setEventId(eventId);
            event.setEventType(eventType);
            event.setRawPayload(rawPayload);
            event.setStatus("RECEIVED");

            eventRepository.saveAndFlush(event);

        } catch (DataIntegrityViolationException duplicate) {

            throw duplicate;

        } catch (IllegalArgumentException error) {

            throw error;

        } catch (Exception error) {

            throw new IllegalArgumentException(
                    "Webhook body is not valid JSON"
            );
        }
    }

    @Scheduled(
            fixedDelayString = "${razorpay.webhook.poll-delay-ms:15000}"
    )
    public void processDueEvents() {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime leaseCutoff =
                now.minusMinutes(LEASE_MINUTES);

        List<Long> ids =
                eventRepository.findDueEventIdsIncludingStale(
                        now,
                        leaseCutoff,
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (Long id : ids) {

            try {
                // Claims the event in a short transaction.
                // Razorpay API calls happen outside that transaction.
                processor.processOne(id);

            } catch (Exception error) {

                // Log only the internal event record ID and exception type.
                // Never log payment IDs, customer data, or raw payloads.
                log.error(
                        "Webhook processing failed. eventRecordId={}, exceptionType={}",
                        id,
                        error.getClass().getSimpleName()
                );
            }
        }
    }
}
