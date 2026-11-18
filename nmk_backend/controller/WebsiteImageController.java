
package nmk_backend.controller;

import nmk_backend.entity.WebsiteImage;
import nmk_backend.repository.WebsiteImageRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class WebsiteImageController {

    private final WebsiteImageRepository websiteImageRepository;

    public WebsiteImageController(
            WebsiteImageRepository websiteImageRepository) {
        this.websiteImageRepository = websiteImageRepository;
    }

    // Public: Get all website images
    @GetMapping("/website-images")
    public List<WebsiteImage> getWebsiteImages() {
        return websiteImageRepository.findAll();
    }

    // Admin: Get all website images
    @GetMapping("/admin/website-images")
    public List<WebsiteImage> getAdminWebsiteImages() {
        return websiteImageRepository.findAll();
    }

    // Admin: Add or update a website image
    @PutMapping("/admin/website-images/{imageKey}")
    public ResponseEntity<WebsiteImage> saveWebsiteImage(
            @PathVariable String imageKey,
            @RequestBody WebsiteImage updatedImage) {

        WebsiteImage image = websiteImageRepository
                .findByImageKey(imageKey)
                .orElseGet(WebsiteImage::new);

        image.setImageKey(imageKey);
        image.setTitle(updatedImage.getTitle());
        image.setImageUrl(updatedImage.getImageUrl());

        return ResponseEntity.ok(
                websiteImageRepository.save(image)
        );
    }
}
