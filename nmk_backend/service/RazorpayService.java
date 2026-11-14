
package nmk_backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;
    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public Order createOrder(int amount) throws Exception {

        RazorpayClient client =
                new RazorpayClient(keyId, keySecret);

        JSONObject options = new JSONObject();
        options.put("amount", amount);
        options.put("currency", "INR");
        options.put("receipt", "nmk_" + System.currentTimeMillis());

        return client.orders.create(options);
    }

    public boolean verifySignature(
            String orderId,
            String paymentId,
            String signature) throws Exception {

        String payload = orderId + "|" + paymentId;

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey = new SecretKeySpec(
                keySecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(secretKey);

        byte[] expected = mac.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
        );

        byte[] actual = hexToBytes(signature);

        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] hexToBytes(String hex) {

        if (hex == null || !hex.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Invalid signature");
        }

        byte[] bytes = new byte[hex.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(
                    hex.substring(i * 2, i * 2 + 2), 16
            );
        }

        return bytes;
    }


    public boolean isPaymentCapturedForOrder(
            String paymentId,
            String expectedOrderId,
            int expectedAmountPaise) throws Exception {

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        com.razorpay.Payment payment =
                client.payments.fetch(paymentId);

        String status = payment.get("status");
        String actualOrderId = payment.get("order_id");

        Number actualAmount = (Number) payment.get("amount");

        return "captured".equals(status)
                && expectedOrderId.equals(actualOrderId)
                && actualAmount.intValue() == expectedAmountPaise;
    }


    public String getKeyId() {
        return keyId;
    }
    public boolean hasUnsettledOrSuccessfulPayment(
            String razorpayOrderId) throws Exception {

        RazorpayClient client =
                new RazorpayClient(keyId, keySecret);

        // Fetch the latest Razorpay order status.
        com.razorpay.Order razorpayOrder =
                client.orders.fetch(razorpayOrderId);

        String orderStatus = razorpayOrder.get("status");

        // A paid order must never have its reservation released.
        if ("paid".equalsIgnoreCase(orderStatus)) {
            return true;
        }

        // Fetch every payment attempt associated with this order.
        List<com.razorpay.Payment> payments =
                client.orders.fetchPayments(razorpayOrderId);

        for (com.razorpay.Payment payment : payments) {

            String status = payment.get("status");

            // Only terminal failed or fully refunded attempts
            // are safe to ignore.
            if (!"failed".equalsIgnoreCase(status)
                    && !"refunded".equalsIgnoreCase(status)) {
                return true;
            }
        }

        // If Razorpay reports attempts but gives no payment records,
        // treat the result as uncertain and retain the reservation.
        Number attempts = razorpayOrder.get("attempts");

        if (attempts != null
                && attempts.intValue() > 0
                && payments.isEmpty()) {
            return true;
        }

        return false;
    }
    public boolean verifyWebhookSignature(
            String rawBody,
            String signature) throws Exception {

        if (rawBody == null || signature == null) {
            return false;
        }

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(secretKey);

        byte[] expected = mac.doFinal(
                rawBody.getBytes(StandardCharsets.UTF_8)
        );

        byte[] actual;

        try {
            actual = hexToBytes(signature);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return MessageDigest.isEqual(expected, actual);
    }
}
