
package nmk_backend.repository;

import nmk_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByTrackingToken(String trackingToken);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.razorpayOrderId = :razorpayOrderId
    """)
    Optional<Order> findByRazorpayOrderIdForUpdate(
            @Param("razorpayOrderId") String razorpayOrderId
    );

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items
        ORDER BY o.orderDate DESC
    """)
    List<Order> findAllWithItems();

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items
        WHERE o.stockReserved = true
          AND o.reservationExpiresAt <= :now
          AND o.paymentStatus = 'PENDING'
    """)
    List<Order> findExpiredReservations(
            @Param("now") LocalDateTime now
    );
}
