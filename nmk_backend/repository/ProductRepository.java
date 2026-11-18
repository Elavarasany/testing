
package nmk_backend.repository;

import nmk_backend.entity.Product;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity - :quantity,
            p.version = p.version + 1
        WHERE p.id = :productId
          AND p.active = true
          AND p.stockQuantity >= :quantity
    """)
    int reserveStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity + :quantity,
            p.version = p.version + 1
        WHERE p.id = :productId
    """)
    int releaseStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );
}
