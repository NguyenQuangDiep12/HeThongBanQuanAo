package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.ProductFilterOptions;
import com.project_shopping.shopee.dto.ApiDtos.ProductPageResponse;
import com.project_shopping.shopee.dto.ApiDtos.ProductResponse;
import com.project_shopping.shopee.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "newest") String sort) {

        return productService.search(
                keyword,
                category != null && !category.isBlank() ? category : type,
                size,
                color,
                minPrice,
                maxPrice,
                inStock,
                page,
                pageSize,
                sort
        );
    }

    @GetMapping("/filters")
    public ProductFilterOptions getFilterOptions() {
        return productService.filterOptions();
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return productService.categories();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductDetail(@PathVariable Long id) {
        return productService.getActive(id);
    }

    @GetMapping("/{id}/related")
    public List<ProductResponse> getRelatedProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int limit) {

        return productService.related(id, limit);
    }
}
