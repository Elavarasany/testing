package nmk_backend.repository;

import nmk_backend.entity.MarketingBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingBannerRepository extends JpaRepository<MarketingBanner, Long> {

    List<MarketingBanner> findAllByOrderByDisplayOrderAsc();

    List<MarketingBanner> findByActiveTrueOrderByDisplayOrderAsc();
}