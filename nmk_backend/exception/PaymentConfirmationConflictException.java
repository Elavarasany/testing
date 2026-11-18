package nmk_backend.exception;

public class PaymentConfirmationConflictException extends RuntimeException {

    public PaymentConfirmationConflictException(String message) {
        super(message);
    }
}
