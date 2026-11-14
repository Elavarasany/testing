
package nmk_backend.repository;

import nmk_backend.entity.WebsiteImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebsiteImageRepository
        extends JpaRepository<WebsiteImage, Long> {

    Optional<WebsiteImage> findByImageKey(String imageKey);
}
