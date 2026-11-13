
package nmk_backend.dto;

public class CreatePaymentResponse {

    private String orderId;
    private int amount;
    private String currency;
    private String keyId;

    public CreatePaymentResponse(String orderId, int amount,
                                 String currency, String keyId) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.keyId = keyId;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getKeyId() {
        return keyId;
    }
}
