
package nmk_backend.repository;

import nmk_backend.entity.RazorpayWebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Long> {

    boolean existsByEventId(String eventId);

    // Existing query: events ready for their first attempt or retry.
    @Query("""
            SELECT e.id
            FROM RazorpayWebhookEvent e
            WHERE e.status IN ('RECEIVED', 'RETRY')
              AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
            ORDER BY e.receivedAt ASC
            """)
    List<Long> findDueEventIds(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    // Finds both due events and PROCESSING events whose worker lease expired.
    // A PROCESSING event with no start timestamp is also eligible for recovery.
    @Query("""
            SELECT e.id
            FROM RazorpayWebhookEvent e
            WHERE (
                e.status IN ('RECEIVED', 'RETRY')
                AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
            )
            OR (
                e.status = 'PROCESSING'
                AND (
                    e.processingStartedAt IS NULL
                    OR e.processingStartedAt <= :leaseCutoff
                )
            )
            ORDER BY e.receivedAt ASC
            """)
    List<Long> findDueEventIdsIncludingStale(
            @Param("now") LocalDateTime now,
            @Param("leaseCutoff") LocalDateTime leaseCutoff,
            Pageable pageable
    );

    // Locks one event row while a short database transaction claims or
    // finalizes that event. Do not hold this lock during external API calls.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM RazorpayWebhookEvent e
            WHERE e.id = :id
            """)
    Optional<RazorpayWebhookEvent> findByIdForUpdate(
            @Param("id") Long id
    );
}
