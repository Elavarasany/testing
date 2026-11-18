
package nmk_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderTrackingResponse(
        String orderStatus,
        BigDecimal totalAmount,
        LocalDateTime orderDate,
        String courierName,
        String trackingNumber,
        String customerName,
        String city,
        List<TrackingItemResponse> items
) {
    public record TrackingItemResponse(
            String productName,
            Integer quantity
    ) {}
}
