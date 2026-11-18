
package nmk_backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.time.LocalDateTime;

@Check(constraints =
        "on_hand_quantity >= 0 " +
                "AND reserved_quantity >= 0 " +
                "AND reserved_quantity <= on_hand_quantity " +
                "AND reorder_level >= 0"
)

@Entity
@Table(
    name = "inventory_balances",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inventory_balance_location_variant",
            columnNames = {"location_id", "variant_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_inventory_balances_variant_id",
            columnList = "variant_id"
        )
    }
)
public class InventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "on_hand_quantity", nullable = false)
    private Integer onHandQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 0;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public InventoryBalance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getOnHandQuantity() {
        return onHandQuantity;
    }

    public void setOnHandQuantity(Integer onHandQuantity) {
        this.onHandQuantity = onHandQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Transient
    public int getAvailableQuantity() {
        return onHandQuantity - reservedQuantity;
    }
}
