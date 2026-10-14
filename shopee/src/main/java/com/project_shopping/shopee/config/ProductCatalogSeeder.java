package com.project_shopping.shopee.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_shopping.shopee.model.Category;
import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.model.ProductVariant;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.repository.CategoryRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Loads the bundled catalog once per missing product without replacing admin-managed data. */
@Component
public class ProductCatalogSeeder implements ApplicationRunner {

    private static final String CATALOG_RESOURCE = "catalog/products-100.json";
    private static final System.Logger LOGGER =
            System.getLogger(ProductCatalogSeeder.class.getName());

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public ProductCatalogSeeder(
            CategoryRepository categories,
            ProductRepository products,
            ObjectMapper objectMapper,
            @Value("${app.bootstrap.product-catalog.enabled:true}") boolean enabled) {
        this.categories = categories;
        this.products = products;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        List<CatalogProductSeed> seeds = loadSeeds();
        Map<String, Category> categoryIndex = new LinkedHashMap<>();
        int inserted = 0;
        int skipped = 0;

        for (CatalogProductSeed seed : seeds) {
            String categoryKey = normalize(seed.category());
            categoryIndex.computeIfAbsent(
                    categoryKey,
                    ignored -> ensureActiveCategory(seed.category()));
        }

        Set<String> existingProductKeys = products.findCatalogProductKeys(
                        categoryIndex.values().stream()
                                .map(Category::getId)
                                .collect(Collectors.toSet()))
                .stream()
                .map(existing -> productKey(existing.getCategoryId(), existing.getName()))
                .collect(Collectors.toCollection(HashSet::new));

        for (CatalogProductSeed seed : seeds) {
            Category category = categoryIndex.get(normalize(seed.category()));
            String productKey = productKey(category.getId(), seed.name());
            if (!existingProductKeys.add(productKey)) {
                skipped++;
                continue;
            }

            BigDecimal price = BigDecimal.valueOf(seed.priceVnd());
            Product product = new Product(
                    seed.name(),
                    seed.description(),
                    price,
                    seed.imageUrl());
            product.setCategory(category);
            product.setStatus(ProductStatus.ACTIVE);

            ProductVariant variant = new ProductVariant(
                    product,
                    seed.size(),
                    seed.color(),
                    seed.stockQuantity(),
                    price);
            product.getVariants().add(variant);
            products.save(product);
            inserted++;
        }

        LOGGER.log(
                System.Logger.Level.INFO,
                "Catalog sản phẩm mẫu: đã thêm {0}/{1} sản phẩm, bỏ qua {2} sản phẩm đã tồn tại.",
                inserted,
                seeds.size(),
                skipped);
    }

    private Category ensureActiveCategory(String name) {
        Category category = categories.findByNameIgnoreCase(name)
                .orElseGet(() -> categories.save(new Category(name, null)));
        if (!category.isActive()) {
            category.setActive(true);
        }
        return category;
    }

    private List<CatalogProductSeed> loadSeeds() {
        try (InputStream input = new ClassPathResource(CATALOG_RESOURCE).getInputStream()) {
            List<CatalogProductSeed> seeds = objectMapper.readValue(
                    input,
                    new TypeReference<>() { });
            validateSeeds(seeds);
            return seeds;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Không thể đọc hoặc kiểm tra dữ liệu catalog tại " + CATALOG_RESOURCE,
                    exception);
        }
    }

    private void validateSeeds(List<CatalogProductSeed> seeds) {
        if (seeds == null || seeds.size() != 100) {
            throw new IllegalStateException("Catalog phải chứa đúng 100 sản phẩm.");
        }

        Set<String> identities = new HashSet<>();
        for (int index = 0; index < seeds.size(); index++) {
            CatalogProductSeed seed = seeds.get(index);
            if (seed == null
                    || isBlank(seed.name()) || seed.name().length() > 180
                    || isBlank(seed.category()) || seed.category().length() > 80
                    || seed.priceVnd() <= 0 || seed.priceVnd() > 9_999_999_999L
                    || isBlank(seed.imageUrl()) || seed.imageUrl().length() > 1000
                    || isBlank(seed.description())
                    || isBlank(seed.size()) || seed.size().length() > 40
                    || isBlank(seed.color()) || seed.color().length() > 60
                    || seed.stockQuantity() < 0) {
                throw new IllegalStateException(
                        "Dữ liệu catalog không hợp lệ tại dòng " + (index + 1) + ".");
            }

            String identity = normalize(seed.category()) + "\u0000" + normalize(seed.name());
            if (!identities.add(identity)) {
                throw new IllegalStateException(
                        "Tên sản phẩm bị trùng trong catalog tại dòng " + (index + 1) + ".");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String productKey(Long categoryId, String name) {
        return categoryId + "\u0000" + normalize(name);
    }

    private record CatalogProductSeed(
            String name,
            String category,
            long priceVnd,
            String imageUrl,
            String size,
            String color,
            int stockQuantity,
            String description) {
    }
}
