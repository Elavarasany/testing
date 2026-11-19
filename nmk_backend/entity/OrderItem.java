
package nmk_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private Long productId;

    private String productName;

    private Integer quantity;

    private BigDecimal price;

    private String prescriptionType;

    private String lensType;

    private Boolean photochromic;

    @Column(name = "prescription_json", columnDefinition = "TEXT")
    private String prescriptionJson;

    // Selected product variant snapshots
    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "variant_sku", length = 100)
    private String variantSku;

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(name = "variant_attributes_json", length = 4000)
    private String variantAttributesJson;

    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPrescriptionType() {
        return prescriptionType;
    }

    public void setPrescriptionType(String prescriptionType) {
        this.prescriptionType = prescriptionType;
    }

    public String getLensType() {
        return lensType;
    }

    public void setLensType(String lensType) {
        this.lensType = lensType;
    }

    public Boolean getPhotochromic() {
        return photochromic;
    }

    public void setPhotochromic(Boolean photochromic) {
        this.photochromic = photochromic;
    }

    public String getPrescriptionJson() {
        return prescriptionJson;
    }

    public void setPrescriptionJson(String prescriptionJson) {
        this.prescriptionJson = prescriptionJson;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getVariantSku() {
        return variantSku;
    }

    public void setVariantSku(String variantSku) {
        this.variantSku = variantSku;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getVariantAttributesJson() {
        return variantAttributesJson;
    }

    public void setVariantAttributesJson(String variantAttributesJson) {
        this.variantAttributesJson = variantAttributesJson;
    }
}
