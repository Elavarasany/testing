package nmk_backend.repository;

import nmk_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items
        ORDER BY o.orderDate DESC
    """)
    List<Order> findAllWithItems();
}