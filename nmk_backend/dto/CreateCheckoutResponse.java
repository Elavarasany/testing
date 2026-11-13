package nmk_backend.dto;

public class CreateCheckoutResponse {

    private Long databaseOrderId;
    private String razorpayOrderId;
    private int amount;
    private String currency;
    private String keyId;

    public CreateCheckoutResponse(
            Long databaseOrderId,
            String razorpayOrderId,
            int amount,
            String currency,
            String keyId) {

        this.databaseOrderId = databaseOrderId;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.currency = currency;
        this.keyId = keyId;
    }

    public Long getDatabaseOrderId() {
        return databaseOrderId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
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