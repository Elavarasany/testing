
package nmk_backend.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminOrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        String prescriptionType,
        String lensType,
        Boolean photochromic,
        Map<String, Object> prescription
) {}
