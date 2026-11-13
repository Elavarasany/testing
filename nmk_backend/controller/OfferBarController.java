
package nmk_backend.controller;

import nmk_backend.entity.OfferBar;
import nmk_backend.repository.OfferBarRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class OfferBarController {

    private final OfferBarRepository offerBarRepository;

    public OfferBarController(OfferBarRepository offerBarRepository) {
        this.offerBarRepository = offerBarRepository;
    }

    // Public: Get the currently valid offer bar
    @GetMapping("/marketing/offer")
    public ResponseEntity<OfferBar> getCurrentOffer() {

        LocalDateTime now = LocalDateTime.now(
                ZoneId.of("Asia/Kolkata")
        );

        return offerBarRepository.findByActiveTrue()
                .stream()
                .filter(offer ->
                        (offer.getStartDateTime() == null ||
                                !now.isBefore(offer.getStartDateTime()))
                                &&
                                (offer.getEndDateTime() == null ||
                                        !now.isAfter(offer.getEndDateTime()))
                )
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // Admin: Get all offer bars
    @GetMapping("/admin/marketing/offers")
    public List<OfferBar> getAllOffers() {
        return offerBarRepository.findAll();
    }

    // Admin: Create offer bar
    @PostMapping("/admin/marketing/offers")
    public OfferBar createOffer(@RequestBody OfferBar offer) {
        offer.setId(null);
        return offerBarRepository.save(offer);
    }

    // Admin: Update offer bar
    @PutMapping("/admin/marketing/offers/{id}")
    public ResponseEntity<OfferBar> updateOffer(
            @PathVariable Long id,
            @RequestBody OfferBar updatedOffer) {

        return offerBarRepository.findById(id)
                .map(offer -> {
                    offer.setMessage(updatedOffer.getMessage());
                    offer.setBackgroundColor(updatedOffer.getBackgroundColor());
                    offer.setTextColor(updatedOffer.getTextColor());
                    offer.setTargetUrl(updatedOffer.getTargetUrl());
                    offer.setStartDateTime(updatedOffer.getStartDateTime());
                    offer.setEndDateTime(updatedOffer.getEndDateTime());
                    offer.setActive(updatedOffer.isActive());

                    return ResponseEntity.ok(
                            offerBarRepository.save(offer)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Admin: Delete offer bar
    @DeleteMapping("/admin/marketing/offers/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {

        if (!offerBarRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        offerBarRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
