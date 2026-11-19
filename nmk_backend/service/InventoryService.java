
package nmk_backend.service;

import nmk_backend.entity.*;
import nmk_backend.exception.*;
import nmk_backend.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class InventoryService {

    private final InventoryLocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;

    public InventoryService(
            InventoryLocationRepository locationRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            InventoryBalanceRepository balanceRepository,
            StockMovementRepository movementRepository
    ) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public InventoryBalance receiveStock(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason
    ) {
        return receiveStock(locationId, variantId, quantity,
                idempotencyKey, actorStaffId, reason, null, null);
    }

    @Transactional
    public InventoryBalance receiveStock(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason,
            String referenceType, Long referenceId
    ) {
        validateQuantity(quantity);
        return execute(locationId, variantId, quantity, idempotencyKey,
                actorStaffId, reason, "RECEIPT", quantity, 0, true,
                referenceType, referenceId);
    }

    @Transactional
    public InventoryBalance reserveStock(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason
    ) {
        return reserveStock(locationId, variantId, quantity,
                idempotencyKey, actorStaffId, reason, null, null);
    }

    @Transactional
    public InventoryBalance reserveStock(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason,
            String referenceType, Long referenceId
    ) {
        validateQuantity(quantity);
        return execute(locationId, variantId, quantity, idempotencyKey,
                actorStaffId, reason, "RESERVATION", 0, quantity, false,
                referenceType, referenceId);
    }

    @Transactional
    public InventoryBalance releaseReservation(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason
    ) {
        return releaseReservation(locationId, variantId, quantity,
                idempotencyKey, actorStaffId, reason, null, null);
    }

    @Transactional
    public InventoryBalance releaseReservation(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason,
            String referenceType, Long referenceId
    ) {
        validateQuantity(quantity);
        return execute(locationId, variantId, quantity, idempotencyKey,
                actorStaffId, reason, "RELEASE", 0, -quantity, false,
                referenceType, referenceId);
    }

    @Transactional
    public InventoryBalance consumeReservation(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason
    ) {
        return consumeReservation(locationId, variantId, quantity,
                idempotencyKey, actorStaffId, reason, null, null);
    }

    @Transactional
    public InventoryBalance consumeReservation(
            Long locationId, Long variantId, int quantity,
            String idempotencyKey, Long actorStaffId, String reason,
            String referenceType, Long referenceId
    ) {
        validateQuantity(quantity);
        return execute(locationId, variantId, quantity, idempotencyKey,
                actorStaffId, reason, "CONSUMPTION", -quantity, -quantity,
                false, referenceType, referenceId);
    }

    private InventoryBalance execute(
            Long locationId,
            Long variantId,
            int quantity,
            String idempotencyKey,
            Long actorStaffId,
            String reason,
            String movementType,
            int quantityDelta,
            int reservedDelta,
            boolean allowBalanceCreation,
            String referenceType,
            Long referenceId
    ) {
        validateIds(locationId, variantId);

        String normalizedKey =
                validateAndNormalizeIdempotencyKey(idempotencyKey);

        validateReference(referenceType, referenceId);

        /*
         * Lock order must remain consistent:
         * location -> product -> variant -> balance.
         */
        InventoryLocation location = locationRepository
                .findByIdForUpdate(locationId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory location not found"
                ));

        Long productId = variantRepository
                .findProductIdByVariantId(variantId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Product variant not found"
                ));

        Product product = productRepository
                .findByIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Parent product not found"
                ));

        ProductVariant variant = variantRepository
                .findByIdForUpdate(variantId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Product variant not found"
                ));

        if (variant.getProduct() == null
                || !Objects.equals(
                productId,
                variant.getProduct().getId()
        )) {
            throw new InventoryConflictException(
                    "Product variant parent changed during inventory operation"
            );
        }

        /*
         * Idempotency is checked before active-status validation.
         *
         * A completed operation must remain safely retryable even if
         * its location, product, or variant was deactivated afterward.
         *
         * Do not weaken these comparisons. Reusing a key for a different
         * operation or reference must always be rejected.
         */
        var existingMovement =
                movementRepository.findByIdempotencyKey(normalizedKey);

        if (existingMovement.isPresent()) {
            StockMovement existing = existingMovement.get();

            boolean sameOperation =
                    existing.getLocation() != null
                            && Objects.equals(
                            existing.getLocation().getId(),
                            locationId
                    )
                            && existing.getVariant() != null
                            && Objects.equals(
                            existing.getVariant().getId(),
                            variantId
                    )
                            && Objects.equals(
                            existing.getMovementType(),
                            movementType
                    )
                            && Objects.equals(
                            existing.getQuantityDelta(),
                            quantityDelta
                    )
                            && Objects.equals(
                            existing.getReservedDelta(),
                            reservedDelta
                    )
                            && Objects.equals(
                            existing.getReferenceType(),
                            referenceType
                    )
                            && Objects.equals(
                            existing.getReferenceId(),
                            referenceId
                    );

            if (!sameOperation) {
                throw new InventoryConflictException(
                        "Idempotency key was already used for another operation or reference"
                );
            }

            return balanceRepository
                    .findByLocationIdAndVariantId(locationId, variantId)
                    .orElseThrow(() -> new InventoryConflictException(
                            "Previous movement exists but its balance is missing"
                    ));
        }

        /*
         * New receipts and reservations must use active records.
         *
         * Releases and consumptions are allowed for inactive records:
         * deactivation must not strand stock already reserved by an order.
         */
        boolean requiresActiveRecords =
                "RECEIPT".equals(movementType)
                        || "RESERVATION".equals(movementType);

        if (requiresActiveRecords) {
            if (!location.isActive()) {
                throw new InvalidInventoryOperationException(
                        "Inventory location is inactive"
                );
            }

            if (!product.isActive()) {
                throw new InvalidInventoryOperationException(
                        "Parent product is inactive"
                );
            }

            if (!variant.isActive()) {
                throw new InvalidInventoryOperationException(
                        "Product variant is inactive"
                );
            }
        }

        InventoryBalance balance = balanceRepository
                .findByLocationAndVariantForUpdate(locationId, variantId)
                .orElse(null);

        if (balance == null) {
            if (!allowBalanceCreation) {
                throw new InventoryNotFoundException(
                        "Inventory balance not found. Receive stock first."
                );
            }

            balance = new InventoryBalance();
            balance.setLocation(location);
            balance.setVariant(variant);
            balance.setOnHandQuantity(0);
            balance.setReservedQuantity(0);
            balance.setReorderLevel(0);
        }

        int currentOnHand = balance.getOnHandQuantity();
        int currentReserved = balance.getReservedQuantity();

        if (quantityDelta == 0 && reservedDelta > 0) {
            int available = currentOnHand - currentReserved;

            if (available < quantity) {
                throw new InsufficientStockException(
                        "Insufficient available stock. Available: " + available
                );
            }
        }

        if (reservedDelta < 0 && currentReserved < quantity) {
            throw new InsufficientStockException(
                    "Reserved quantity is insufficient. Reserved: "
                            + currentReserved
            );
        }

        int newOnHand;
        int newReserved;

        try {
            newOnHand = Math.addExact(currentOnHand, quantityDelta);
            newReserved = Math.addExact(currentReserved, reservedDelta);
        } catch (ArithmeticException exception) {
            throw new InvalidInventoryOperationException(
                    "Inventory quantity exceeds the supported limit"
            );
        }

        if (newOnHand < 0
                || newReserved < 0
                || newReserved > newOnHand) {
            throw new InvalidInventoryOperationException(
                    "Operation would create an invalid inventory balance"
            );
        }

        balance.setOnHandQuantity(newOnHand);
        balance.setReservedQuantity(newReserved);

        balanceRepository.saveAndFlush(balance);

        StockMovement movement = new StockMovement();
        movement.setLocation(location);
        movement.setVariant(variant);
        movement.setMovementType(movementType);
        movement.setQuantityDelta(quantityDelta);
        movement.setReservedDelta(reservedDelta);
        movement.setOnHandAfter(newOnHand);
        movement.setReservedAfter(newReserved);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setIdempotencyKey(normalizedKey);
        movement.setActorStaffId(actorStaffId);
        movement.setReason(reason);

        movementRepository.saveAndFlush(movement);

        return balance;
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidInventoryOperationException(
                    "Quantity must be greater than zero"
            );
        }
    }

    private void validateIds(Long locationId, Long variantId) {
        if (locationId == null || variantId == null
                || locationId <= 0 || variantId <= 0) {
            throw new InvalidInventoryOperationException(
                    "Valid location and variant IDs are required"
            );
        }
    }

    private String validateAndNormalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.trim().length() > 150) {
            throw new InvalidInventoryOperationException(
                    "A valid idempotency key is required (maximum 150 characters)"
            );
        }

        return key.trim();
    }

    private void validateReference(String referenceType, Long referenceId) {
        if (referenceType != null
                && (referenceType.isBlank()
                || referenceType.trim().length() > 40)) {
            throw new InvalidInventoryOperationException(
                    "Reference type must contain 1 to 40 characters"
            );
        }

        if (referenceId != null && referenceId <= 0) {
            throw new InvalidInventoryOperationException(
                    "Reference ID must be greater than zero"
            );
        }

        if ((referenceType == null) != (referenceId == null)) {
            throw new InvalidInventoryOperationException(
                    "Reference type and reference ID must be supplied together"
            );
        }
    }
}
