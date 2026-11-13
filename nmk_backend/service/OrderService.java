
package nmk_backend.service;

import nmk_backend.dto.CreateOrderRequest;
import nmk_backend.dto.OrderItemRequest;
import nmk_backend.entity.Order;
import nmk_backend.entity.OrderItem;
import nmk_backend.entity.Product;
import nmk_backend.repository.OrderRepository;
import nmk_backend.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createPendingOrder(CreateOrderRequest request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();

        order.setCustomerName(request.getCustomerName());
        order.setEmail(request.getEmail());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPincode(request.getPincode());
        order.setPaymentStatus("PENDING");

        BigDecimal total = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        // Combine quantities when the same product appears multiple times
        Map<Long, Integer> reservedQuantities = new LinkedHashMap<>();

        for (OrderItemRequest itemRequest : request.getItems()) {

            if (itemRequest.getProductId() == null ||
                    itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() <= 0) {

                throw new IllegalArgumentException("Invalid cart item");
            }

            Product product = productRepository.findById(
                    itemRequest.getProductId()
            ).orElseThrow(() ->
                    new IllegalArgumentException("Product not found")
            );

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        product.getName() + " is unavailable"
                );
            }

            // Aggregate quantity for atomic stock reservation
            reservedQuantities.merge(
                    product.getId(),
                    itemRequest.getQuantity(),
                    Integer::sum
            );

            // Start with the frame/product price
            BigDecimal unitPrice = product.getPrice();

            boolean isEyeglasses =
                    "Eyeglasses".equalsIgnoreCase(product.getCategory());

            String prescription = itemRequest.getPrescriptionType();
            String lens = itemRequest.getLensType();

            boolean photochromic = Boolean.TRUE.equals(
                    itemRequest.getPhotochromic()
            );

            if (isEyeglasses) {

                if (prescription == null || prescription.isBlank()) {
                    throw new IllegalArgumentException(
                            "Please select a Power Type for Eyeglasses"
                    );
                }

                BigDecimal lensPrice = BigDecimal.ZERO;

                switch (prescription) {

                    case "Frame Only":
                        lensPrice = BigDecimal.ZERO;
                        break;

                    case "Single Vision":
                    case "Progressive":
                    case "Bifocal":

                        if (lens == null || lens.isBlank()) {
                            throw new IllegalArgumentException(
                                    "Please select a lens type"
                            );
                        }

                        switch (prescription) {

                            case "Single Vision":

                                switch (lens) {
                                    case "ClearGuard":
                                        lensPrice = BigDecimal.valueOf(899);
                                        break;

                                    case "ClearGuard Blue":
                                        lensPrice = BigDecimal.valueOf(1649);
                                        break;

                                    case "ClearGuard Plus":
                                        lensPrice = BigDecimal.valueOf(1989);
                                        break;

                                    case "ClearGuard NightDrive":
                                        lensPrice = BigDecimal.valueOf(2899);
                                        break;

                                    default:
                                        throw new IllegalArgumentException(
                                                "Invalid lens type"
                                        );
                                }

                                if (photochromic) {
                                    lensPrice = lensPrice.add(
                                            BigDecimal.valueOf(689)
                                    );
                                }

                                break;

                            case "Progressive":

                                switch (lens) {
                                    case "ClearGuard":
                                        lensPrice = BigDecimal.valueOf(1799);
                                        break;

                                    case "ClearGuard Blue":
                                        lensPrice = BigDecimal.valueOf(2449);
                                        break;

                                    case "ClearGuard Plus":
                                        lensPrice = BigDecimal.valueOf(2989);
                                        break;

                                    case "ClearGuard NightDrive":
                                        lensPrice = BigDecimal.valueOf(3899);
                                        break;

                                    default:
                                        throw new IllegalArgumentException(
                                                "Invalid lens type"
                                        );
                                }

                                if (photochromic) {
                                    lensPrice = lensPrice.add(
                                            BigDecimal.valueOf(989)
                                    );
                                }

                                break;

                            case "Bifocal":

                                switch (lens) {
                                    case "ClearGuard":
                                        lensPrice = BigDecimal.valueOf(1399);
                                        break;

                                    case "ClearGuard Blue":
                                        lensPrice = BigDecimal.valueOf(1849);
                                        break;

                                    case "ClearGuard Plus":
                                        lensPrice = BigDecimal.valueOf(2489);
                                        break;

                                    case "ClearGuard NightDrive":
                                        lensPrice = BigDecimal.valueOf(2999);
                                        break;

                                    default:
                                        throw new IllegalArgumentException(
                                                "Invalid lens type"
                                        );
                                }

                                if (photochromic) {
                                    lensPrice = lensPrice.add(
                                            BigDecimal.valueOf(989)
                                    );
                                }

                                break;
                        }

                        break;

                    default:
                        throw new IllegalArgumentException(
                                "Invalid Power Type"
                        );
                }

                // Add lens charges to the frame price
                unitPrice = unitPrice.add(lensPrice);

            } else if (
                    prescription != null ||
                            lens != null ||
                            photochromic
            ) {
                throw new IllegalArgumentException(
                        "Lens customization is only available for Eyeglasses"
                );
            }

            // Calculate total for this item
            BigDecimal itemTotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            total = total.add(itemTotal);

            // Create order item
            OrderItem orderItem = new OrderItem();

            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());

            // Store final unit price, including lens charges
            orderItem.setPrice(unitPrice);

            // Store lens selections for Eyeglasses
            if (isEyeglasses) {

                orderItem.setPrescriptionType(prescription);

                if ("Frame Only".equals(prescription)) {
                    orderItem.setLensType(null);
                    orderItem.setPhotochromic(false);
                } else {
                    orderItem.setLensType(lens);
                    orderItem.setPhotochromic(photochromic);
                }
            }

            orderItem.setOrder(order);

            orderItems.add(orderItem);
        }

        // Reserve stock atomically for every product
        for (Map.Entry<Long, Integer> entry :
                reservedQuantities.entrySet()) {

            int updatedRows = productRepository.reserveStock(
                    entry.getKey(),
                    entry.getValue()
            );

            if (updatedRows != 1) {
                throw new IllegalArgumentException(
                        "Insufficient stock for one or more products. " +
                                "Please refresh your cart and try again."
                );
            }
        }

        // Set order details
        order.setTotalAmount(total);
        order.setItems(orderItems);

        // Set 15-minute stock reservation
        order.setStockReserved(true);

        order.setReservationExpiresAt(
                LocalDateTime.now().plusMinutes(15)
        );

        return orderRepository.save(order);
    }

    @Transactional
    public void releaseReservation(Long orderId) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );

        releaseReservationForOrder(order);
    }

    @Transactional
    public void cancelPendingOrder(Long orderId) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found")
                );

        if (!"PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Only pending orders can be cancelled through this action"
            );
        }

        if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException(
                    "This order is not an unpaid pending order"
            );
        }

        if (order.isStockReserved()) {
            releaseReservationForOrder(order);
        } else {
            order.setPaymentStatus("FAILED");
            order.setOrderStatus("CANCELLED");
            order.setReservationExpiresAt(null);
            orderRepository.save(order);
        }
    }

    private void releaseReservationForOrder(Order order) {

        // Prevent stock from being released more than once.
        if (!order.isStockReserved()) {
            return;
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            quantities.merge(
                    item.getProductId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {

            int updatedRows = productRepository.releaseStock(
                    entry.getKey(),
                    entry.getValue()
            );

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Could not release stock for product ID: "
                                + entry.getKey()
                );
            }
        }

        order.setStockReserved(false);
        order.setReservationExpiresAt(null);
        order.setPaymentStatus("FAILED");
        order.setOrderStatus("CANCELLED");

        orderRepository.save(order);
    }

}
