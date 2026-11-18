
package nmk_backend.repository;

import nmk_backend.entity.AdminActionAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AdminActionAuthorizationRepository
        extends JpaRepository<AdminActionAuthorization, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM AdminActionAuthorization a
        WHERE a.tokenHash = :tokenHash
    """)
    Optional<AdminActionAuthorization> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
