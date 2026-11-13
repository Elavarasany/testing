
package nmk_backend.dto;

public class OrderItemRequest {

    private Long productId;
    private Integer quantity;

    private String prescriptionType;
    private String lensType;
    private Boolean photochromic;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
}
