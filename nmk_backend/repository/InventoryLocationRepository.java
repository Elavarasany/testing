package nmk_backend.repository;

import nmk_backend.entity.InventoryLocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryLocationRepository
        extends JpaRepository<InventoryLocation, Long> {

    Optional<InventoryLocation> findByCode(String code);

    List<InventoryLocation> findByActiveTrue();

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLocation l WHERE l.id = :id")
    Optional<InventoryLocation> findByIdForUpdate(@Param("id") Long id);
}