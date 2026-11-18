
package nmk_backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.time.LocalDateTime;

@Check(constraints =
        "on_hand_after >= 0 " +
                "AND reserved_after >= 0 " +
                "AND reserved_after <= on_hand_after"
)

@Entity
@Table(
    name = "stock_movements",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_stock_movements_idempotency",
            columnNames = "idempotency_key"
        )
    },
    indexes = {
        @Index(
            name = "idx_stock_movements_location_variant",
            columnList = "location_id, variant_id, created_at"
        ),
        @Index(
            name = "idx_stock_movements_reference",
            columnList = "reference_type, reference_id"
        )
    }
)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "movement_type", nullable = false, length = 40)
    private String movementType;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta = 0;

    @Column(name = "reserved_delta", nullable = false)
    private Integer reservedDelta = 0;

    @Column(name = "on_hand_after", nullable = false)
    private Integer onHandAfter;

    @Column(name = "reserved_after", nullable = false)
    private Integer reservedAfter;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "idempotency_key", nullable = false, length = 150)
    private String idempotencyKey;

    // Keep this as a scalar until a StaffAccount entity is implemented.
    @Column(name = "actor_staff_id")
    private Long actorStaffId;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StockMovement() {
    }

    public Long getId() {
        return id;
    }

    public InventoryLocation getLocation() {
        return location;
    }

    public void setLocation(InventoryLocation location) {
        this.location = location;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public Integer getReservedDelta() {
        return reservedDelta;
    }

    public void setReservedDelta(Integer reservedDelta) {
        this.reservedDelta = reservedDelta;
    }

    public Integer getOnHandAfter() {
        return onHandAfter;
    }

    public void setOnHandAfter(Integer onHandAfter) {
        this.onHandAfter = onHandAfter;
    }

    public Integer getReservedAfter() {
        return reservedAfter;
    }

    public void setReservedAfter(Integer reservedAfter) {
        this.reservedAfter = reservedAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getActorStaffId() {
        return actorStaffId;
    }

    public void setActorStaffId(Long actorStaffId) {
        this.actorStaffId = actorStaffId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
