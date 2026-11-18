
package nmk_backend.repository;

import nmk_backend.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<StockMovement> findByLocationIdAndVariantIdOrderByCreatedAtDesc(
            Long locationId,
            Long variantId
    );

    List<StockMovement> findByReferenceTypeAndReferenceId(
            String referenceType,
            Long referenceId
    );
}
