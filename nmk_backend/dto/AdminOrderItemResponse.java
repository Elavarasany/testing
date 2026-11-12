package nmk_backend.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price
) {}