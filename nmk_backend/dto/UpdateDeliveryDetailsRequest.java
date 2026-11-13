
package nmk_backend.dto;

public record UpdateDeliveryDetailsRequest(
        String courierName,
        String trackingNumber
) {}
