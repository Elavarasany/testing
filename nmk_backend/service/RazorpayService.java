
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

    /**
     * Check whether payment credentials are configured.
     * Call this before creating an order or reserving stock.
     */
    public void requirePaymentCredentials() {

        if (keyId == null || keyId.isBlank()
                || keySecret == null || keySecret.isBlank()) {

            throw new nmk_backend.exception.PaymentConfigurationException(
                    "Razorpay payment credentials are not configured"
            );
        }
    }

    /**
     * Create a Razorpay payment order.
     */
    public Order createOrder(int amount) throws Exception {

        requirePaymentCredentials();

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        RazorpayClient client =
                new RazorpayClient(keyId, keySecret);

        JSONObject options = new JSONObject();
        options.put("amount", amount);
        options.put("currency", "INR");
        options.put("receipt", "nmk_" + System.currentTimeMillis());

        return client.orders.create(options);
    }

    /**
     * Verify Razorpay checkout payment signature.
     */
    public boolean verifySignature(
            String orderId,
            String paymentId,
            String signature) throws Exception {

        requirePaymentCredentials();

        if (orderId == null || orderId.isBlank()
                || paymentId == null || paymentId.isBlank()) {
            return false;
        }

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

        byte[] actual;

        try {
            actual = hexToBytes(signature);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * Convert a hexadecimal signature into bytes.
     */
    private byte[] hexToBytes(String hex) {

        if (hex == null || !hex.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Invalid signature");
        }

        byte[] bytes = new byte[hex.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(
                    hex.substring(i * 2, i * 2 + 2),
                    16
            );
        }

        return bytes;
    }

    /**
     * Verify that a Razorpay payment is captured,
     * belongs to the expected order, and has the expected amount.
     */
    public boolean isPaymentCapturedForOrder(
            String paymentId,
            String expectedOrderId,
            int expectedAmountPaise) throws Exception {

        requirePaymentCredentials();

        if (paymentId == null || paymentId.isBlank()
                || expectedOrderId == null || expectedOrderId.isBlank()
                || expectedAmountPaise <= 0) {
            return false;
        }

        RazorpayClient client =
                new RazorpayClient(keyId, keySecret);

        com.razorpay.Payment payment =
                client.payments.fetch(paymentId);

        String status = payment.get("status");
        String actualOrderId = payment.get("order_id");

        Object amountValue = payment.get("amount");
        String actualCurrency = payment.get("currency");

        Integer actualAmount = parseExactPaiseAmount(amountValue);

        return "captured".equals(status)
                && expectedOrderId.equals(actualOrderId)
                && "INR".equalsIgnoreCase(actualCurrency)
                && actualAmount != null
                && actualAmount == expectedAmountPaise;
    }
    private Integer parseExactPaiseAmount(Object value) {

        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Number number) {
                long amount = new java.math.BigDecimal(number.toString())
                        .longValueExact();

                if (amount <= 0 || amount > Integer.MAX_VALUE) {
                    return null;
                }

                return (int) amount;
            }

            if (value instanceof String string) {
                long amount = Long.parseLong(string);

                if (amount <= 0 || amount > Integer.MAX_VALUE) {
                    return null;
                }

                return (int) amount;
            }

        } catch (NumberFormatException | ArithmeticException ignored) {
            return null;
        }

        return null;
    }

    /**
     * Return the public Razorpay key ID for frontend checkout.
     */
    public String getKeyId() {

        requirePaymentCredentials();

        return keyId;
    }

    /**
     * Check whether a Razorpay order has a successful,
     * pending, or otherwise unsettled payment attempt.
     *
     * Failures in this method must not cause stock release.
     */
    public boolean hasUnsettledOrSuccessfulPayment(
            String razorpayOrderId) throws Exception {

        requirePaymentCredentials();

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay order ID is missing"
            );
        }

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

    /**
     * Verify webhook signature using the raw request body.
     */
    public boolean verifyWebhookSignature(
            String rawBody,
            String signature) throws Exception {

        return rawBody != null && verifyWebhookSignature(
                rawBody.getBytes(StandardCharsets.UTF_8),
                signature
        );
    }

    /**
     * Verify webhook HMAC-SHA256 signature.
     */
    public boolean verifyWebhookSignature(
            byte[] rawBody,
            String signature) throws Exception {

        if (rawBody == null || signature == null
                || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(secretKey);

        byte[] expected = mac.doFinal(rawBody);
        byte[] actual;

        try {
            actual = hexToBytes(signature);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return MessageDigest.isEqual(expected, actual);
    }
}
