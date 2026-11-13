
package nmk_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.auth-key:TEST_AUTH_KEY}")
    private String authKey;

    @Value("${sms.sender-id:NMKOPT}")
    private String senderId;

    @Value("${sms.flow-id:TEST_FLOW_ID}")
    private String flowId;

    public void sendOrderNotification(
            String phone,
            String orderId,
            String status,
            String courierName,
            String trackingNumber,
            String trackingUrl) {

        if (!smsEnabled) {
            System.out.println("\n========== TEST SMS ==========");
            System.out.println("Phone: " + phone);
            System.out.println("Order: " + orderId);
            System.out.println("Status: " + status);
            System.out.println("Courier: " + courierName);
            System.out.println("Tracking Number: " + trackingNumber);
            System.out.println("Tracking URL: " + trackingUrl);
            System.out.println("SMS NOT SENT - TEST MODE");
            System.out.println("=============================\n");
            return;
        }

        // Real MSG91 integration will be enabled later.
        throw new IllegalStateException(
                "MSG91 credentials and live integration are not configured"
        );
    }
}
