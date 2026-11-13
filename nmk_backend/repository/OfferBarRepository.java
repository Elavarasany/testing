package nmk_backend.repository;

import nmk_backend.entity.OfferBar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferBarRepository extends JpaRepository<OfferBar, Long> {

    List<OfferBar> findByActiveTrue();
}