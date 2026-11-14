
package nmk_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        String prescriptionType,
        String lensType,
        Boolean photochromic,
        JsonNode prescription
) {}
