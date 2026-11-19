package nmk_backend.controller;

import nmk_backend.entity.MarketingBanner;
import nmk_backend.repository.MarketingBannerRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class MarketingBannerController {

    private final MarketingBannerRepository bannerRepository;

    public MarketingBannerController(MarketingBannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    // Public: Get active banners for customer website
    @GetMapping("/marketing/banners")
    public List<MarketingBanner> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    // Admin: Get all banners
    @GetMapping("/admin/marketing/banners")
    public List<MarketingBanner> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    // Admin: Add banner
    @PostMapping("/admin/marketing/banners")
    public MarketingBanner addBanner(@RequestBody MarketingBanner banner) {
        banner.setId(null);
        return bannerRepository.save(banner);
    }

    // Admin: Update banner
    @PutMapping("/admin/marketing/banners/{id}")
    public ResponseEntity<MarketingBanner> updateBanner(
            @PathVariable Long id,
            @RequestBody MarketingBanner updatedBanner) {

        return bannerRepository.findById(id)
                .map(banner -> {
                    banner.setTitle(updatedBanner.getTitle());
                    banner.setImageUrl(updatedBanner.getImageUrl());
                    banner.setTargetUrl(updatedBanner.getTargetUrl());
                    banner.setPlacement(updatedBanner.getPlacement());
                    banner.setActive(updatedBanner.isActive());
                    banner.setDisplayOrder(updatedBanner.getDisplayOrder());

                    return ResponseEntity.ok(bannerRepository.save(banner));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Admin: Delete banner
    @DeleteMapping("/admin/marketing/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        if (!bannerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        bannerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}