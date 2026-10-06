package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.*;
import com.project_shopping.shopee.model.*;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.ProductSpecifications;
import com.project_shopping.shopee.repository.VariantRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private static final int MAX_PAGE = 10_000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository products;
    private final VariantRepository variants;
    private final CategoryService categories;

    public ProductService(
            ProductRepository products,
            VariantRepository variants,
            CategoryService categories
    ) {
        this.products = products;
        this.variants = variants;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<String> categories() {

        return products.findDistinctCategories(
                ProductStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public ProductPageResponse search(
            String keyword,
            String category,
            String size,
            String color,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            int page,
            int pageSize,
            String sortKey
    ) {

        validatePagination(page, pageSize);

        if (
                (minPrice != null && minPrice.signum() < 0)
                        || (maxPrice != null && maxPrice.signum() < 0)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khoảng giá lọc phải bằng hoặc lớn hơn 0."
            );
        }

        if (
                minPrice != null
                        && maxPrice != null
                        && minPrice.compareTo(maxPrice) > 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá thấp nhất không được lớn hơn giá cao nhất."
            );
        }

        Sort sort = switch (
                sortKey == null
                        ? "newest"
                        : sortKey.toLowerCase(Locale.ROOT)
        ) {
            case "newest" ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "createdAt"
                    );

            case "price_asc" ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "price"
                    );

            case "price_desc" ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "price"
                    );

            case "name_asc" ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "name"
                    );

            case "name_desc" ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "name"
                    );

            default ->
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Thứ tự sắp xếp không hợp lệ. Hãy chọn mới nhất, giá tăng dần, giá giảm dần, tên A-Z hoặc tên Z-A."
                    );
        };

        Page<Product> result = products.findAll(
                ProductSpecifications.catalog(
                        keyword,
                        category,
                        size,
                        color,
                        minPrice,
                        maxPrice,
                        inStock
                ),
                PageRequest.of(
                        page,
                        pageSize,
                        sort
                )
        );

        return new ProductPageResponse(
                result.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    private void validatePagination(int page, int pageSize) {

        if (page < 0 || page > MAX_PAGE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số trang phải nằm trong khoảng từ 0 đến " + MAX_PAGE + "."
            );
        }

        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số sản phẩm mỗi trang phải nằm trong khoảng từ 1 đến " + MAX_PAGE_SIZE + "."
            );
        }
    }

    @Transactional(readOnly = true)
    public ProductFilterOptions filterOptions() {

        return new ProductFilterOptions(
                products.findDistinctCategories(
                        ProductStatus.ACTIVE
                ),
                products.findAvailableSizes(
                        ProductStatus.ACTIVE
                ),
                products.findAvailableColors(
                        ProductStatus.ACTIVE
                ),
                products.findMinAvailablePrice(
                        ProductStatus.ACTIVE
                ),
                products.findMaxAvailablePrice(
                        ProductStatus.ACTIVE
                )
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> related(
            Long id,
            int limit
    ) {

        Product product = products
                .findWithVariantsByIdAndStatusAndCategory_ActiveTrue(
                        id,
                        ProductStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sản phẩm."
                        )
                );

        if (limit < 1 || limit > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng sản phẩm liên quan phải nằm trong khoảng từ 1 đến 20."
            );
        }

        return products
                .findByStatusAndCategory_IdAndCategory_ActiveTrueAndIdNotOrderByCreatedAtDesc(
                        ProductStatus.ACTIVE,
                        product.getCategory().getId(),
                        id
                )
                .stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getActive(Long id) {

        return toResponse(
                products
                        .findWithVariantsByIdAndStatusAndCategory_ActiveTrue(
                                id,
                                ProductStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Không tìm thấy sản phẩm."
                                )
                        )
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {

        return products
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {

        Product product = new Product(
                request.name().trim(),
                request.description(),
                request.price(),
                request.imageUrl()
        );

        product.setCategory(categories.requireActive(request.categoryId()));

        if (request.status() != null) {
            product.setStatus(request.status());
        }

        return toResponse(
                products.save(product)
        );
    }

    @Transactional
    public ProductResponse update(
            Long id,
            ProductRequest request
    ) {

        Product product = products
                .findWithVariantsById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sản phẩm."
                        )
                );

        product.setName(
                request.name().trim()
        );

        product.setDescription(
                request.description()
        );

        product.setPrice(
                request.price()
        );

        product.setImageUrl(
                request.imageUrl()
        );

        product.setCategory(categories.requireActive(request.categoryId()));

        if (request.status() != null) {
            product.setStatus(
                    request.status()
            );
        }

        return toResponse(product);
    }

    @Transactional
    public void deactivate(Long id) {

        Product product = products
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sản phẩm."
                        )
                );

        product.setStatus(
                ProductStatus.INACTIVE
        );
    }

    @Transactional
    public VariantResponse addVariant(
            Long productId,
            VariantRequest request
    ) {

        Product product = products
                .findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sản phẩm."
                        )
                );

        ProductVariant variant = new ProductVariant(
                product,
                request.size().trim(),
                request.color().trim(),
                request.stockQuantity(),
                request.price()
        );

        return toVariant(
                variants.save(variant)
        );
    }

    @Transactional
    public VariantResponse updateVariant(
            Long productId,
            Long variantId,
            VariantRequest request
    ) {

        ProductVariant variant = variants
                .findByIdForUpdate(variantId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phân loại sản phẩm."
                        )
                );

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy phân loại sản phẩm."
            );
        }

        variant.setSize(
                request.size().trim()
        );

        variant.setColor(
                request.color().trim()
        );

        variant.setStockQuantity(
                request.stockQuantity()
        );

        variant.setPrice(
                request.price()
        );

        return toVariant(variant);
    }

    @Transactional
    public void deleteVariant(
            Long productId,
            Long variantId
    ) {

        ProductVariant variant = variants
                .findById(variantId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phân loại sản phẩm."
                        )
                );

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy phân loại sản phẩm."
            );
        }

        try {
            variants.delete(variant);
            variants.flush();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phân loại sản phẩm đang được sử dụng trong giỏ hàng hoặc đơn hàng nên không thể xóa. Hãy đặt số lượng tồn kho về 0.",
                    ex
            );
        }
    }

    private ProductResponse toResponse(Product p) {

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getDescription(),
                p.getPrice(),
                p.getImageUrl(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getVariants()
                        .stream()
                        .map(this::toVariant)
                        .toList()
        );
    }

    private VariantResponse toVariant(ProductVariant v) {

        return new VariantResponse(
                v.getId(),
                v.getSize(),
                v.getColor(),
                v.getStockQuantity(),
                v.getPrice()
        );
    }
}
