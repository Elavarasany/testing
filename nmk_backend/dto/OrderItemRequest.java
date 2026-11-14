
package nmk_backend.dto;

import java.util.Map;

public class OrderItemRequest {

    private Long productId;
    private Integer quantity;

    private String prescriptionType;
    private String lensType;
    private Boolean photochromic;

    // Customer's prescription: either {mode: "image", imageUrl} or {mode: "manual", right, left, ...}
    private Map<String, Object> prescription;
    private String prescriptionFileName;

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

    public Map<String, Object> getPrescription() {
        return prescription;
    }

    public void setPrescription(Map<String, Object> prescription) {
        this.prescription = prescription;
    }

    public String getPrescriptionFileName() {
        return prescriptionFileName;
    }

    public void setPrescriptionFileName(String prescriptionFileName) {
        this.prescriptionFileName = prescriptionFileName;
    }
}
