
package nmk_backend.service;

import nmk_backend.entity.Product;
import nmk_backend.repository.ProductRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get all active products for customers
    public List<Product> getActiveProducts() {
        return productRepository.findAll()
                .stream()
                .filter(Product::isActive)
                .toList();
    }

    // Get all products for admin
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get product by ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with ID: " + id));
    }

    // Add new product
    public Product addProduct(Product product) {

        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (product.getPrice() == null || product.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Valid product price is required");
        }

        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Valid stock quantity is required");
        }

        product.setId(null);

        return productRepository.save(product);
    }

    // Update existing product
    public Product updateProduct(Long id, Product updatedProduct) {

        Product existingProduct = getProductById(id);

        if (updatedProduct.getName() == null ||
                updatedProduct.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (updatedProduct.getPrice() == null ||
                updatedProduct.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Valid product price is required");
        }

        if (updatedProduct.getStockQuantity() == null ||
                updatedProduct.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Valid stock quantity is required");
        }

        existingProduct.setName(updatedProduct.getName());
        existingProduct.setGender(updatedProduct.getGender());
        existingProduct.setCategory(updatedProduct.getCategory());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setStockQuantity(updatedProduct.getStockQuantity());
        existingProduct.setImageUrl(updatedProduct.getImageUrl());
        existingProduct.setSpecifications(updatedProduct.getSpecifications());
        existingProduct.setActive(updatedProduct.isActive());

        return productRepository.save(existingProduct);
    }

    // Delete product
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
