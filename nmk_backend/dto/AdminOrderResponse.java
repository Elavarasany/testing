package nmk_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        String customerName,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String pincode,
        BigDecimal totalAmount,
        String paymentStatus,
        String orderStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime orderDate,
        List<AdminOrderItemResponse> items
) {}