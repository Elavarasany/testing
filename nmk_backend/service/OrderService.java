
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
import java.util.ArrayList;
import java.util.List;

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

            if (product.getStockQuantity() == null ||
                    product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + product.getName()
                );
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        product.getName() + " is unavailable"
                );
            }

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
                        // No lens charges
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

        order.setTotalAmount(total);
        order.setItems(orderItems);

        return orderRepository.save(order);
    }
}
