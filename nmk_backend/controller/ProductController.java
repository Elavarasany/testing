
package nmk_backend.controller;
import nmk_backend.service.ProductService;

import nmk_backend.entity.Product;
import nmk_backend.repository.ProductRepository;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    public ProductController(
            ProductRepository productRepository,
            ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    // Get all active products
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .filter(Product::isActive)
                .toList();
    }

    // Add a new product
    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // Update existing product
    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    // Get product by ID
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}
