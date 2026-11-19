
package nmk_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String email;
    private String phone;

    @Column(length = 1000)
    private String address;

    private String city;
    private String state;
    private String pincode;

    private BigDecimal totalAmount;

    private String paymentStatus;

    @Column(name = "payment_creation_status", length = 30, nullable = false)
    private String paymentCreationStatus = "NOT_STARTED";

    private String orderStatus = "PENDING";

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;

    @Column(name = "stock_reserved", nullable = false)
    private boolean stockReserved = false;

    // Null on existing orders; new orders identify their reservation system.
    @Column(name = "reservation_type", length = 30)
    private String reservationType;

    // Snapshot of the inventory location used for this order.
    @Column(name = "inventory_location_id")
    private Long inventoryLocationId;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    // Delivery details
    private String courierName;
    private String trackingNumber;

    // Unique token for public order tracking
    @Column(unique = true, length = 64)
    private String trackingToken;

    private LocalDateTime orderDate;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }

        if (orderStatus == null) {
            orderStatus = "PENDING";
        }

        if (paymentCreationStatus == null
                || paymentCreationStatus.isBlank()) {
            paymentCreationStatus = "NOT_STARTED";
        }

        if (trackingToken == null) {
            trackingToken = UUID.randomUUID()
                    .toString()
                    .replace("-", "");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentCreationStatus() {
        return paymentCreationStatus == null
                ? "NOT_STARTED"
                : paymentCreationStatus;
    }

    public void setPaymentCreationStatus(String paymentCreationStatus) {
        this.paymentCreationStatus = paymentCreationStatus;
    }

    public String getOrderStatus() {
        return orderStatus == null ? "PENDING" : orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public boolean isStockReserved() {
        return stockReserved;
    }

    public void setStockReserved(boolean stockReserved) {
        this.stockReserved = stockReserved;
    }

    public String getReservationType() {
        return reservationType;
    }

    public void setReservationType(String reservationType) {
        this.reservationType = reservationType;
    }

    public Long getInventoryLocationId() {
        return inventoryLocationId;
    }

    public void setInventoryLocationId(Long inventoryLocationId) {
        this.inventoryLocationId = inventoryLocationId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getTrackingToken() {
        return trackingToken;
    }

    public void setTrackingToken(String trackingToken) {
        this.trackingToken = trackingToken;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
