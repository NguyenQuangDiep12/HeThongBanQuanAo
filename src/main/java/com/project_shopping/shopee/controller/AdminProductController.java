package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.ProductRequest;
import com.project_shopping.shopee.dto.ApiDtos.ProductResponse;
import com.project_shopping.shopee.dto.ApiDtos.VariantRequest;
import com.project_shopping.shopee.dto.ApiDtos.VariantResponse;
import com.project_shopping.shopee.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @Valid @RequestBody ProductRequest request) {

        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Long id) {
        productService.deactivate(id);
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody VariantRequest request) {

        return productService.addVariant(productId, request);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public VariantResponse updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody VariantRequest request) {

        return productService.updateVariant(
                productId,
                variantId,
                request
        );
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {

        productService.deleteVariant(productId, variantId);
    }
}