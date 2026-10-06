package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.model.ProductVariant;
import com.project_shopping.shopee.model.enums.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> catalog(
            String keyword,
            String category,
            String size,
            String color,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            addActiveStatusFilter(
                    root,
                    criteriaBuilder,
                    predicates
            );

            predicates.add(
                    criteriaBuilder.isTrue(
                            root.get("category").get("active")
                    )
            );

            addKeywordFilter(
                    root,
                    query,
                    criteriaBuilder,
                    predicates,
                    keyword
            );

            addCategoryFilter(
                    root,
                    criteriaBuilder,
                    predicates,
                    category
            );

            addPriceFilter(
                    root,
                    criteriaBuilder,
                    predicates,
                    minPrice,
                    maxPrice
            );

            addVariantFilter(
                    root,
                    query,
                    criteriaBuilder,
                    predicates,
                    size,
                    color,
                    inStock
            );

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static void addActiveStatusFilter(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates) {

        predicates.add(
                criteriaBuilder.equal(
                        root.get("status"),
                        ProductStatus.ACTIVE
                )
        );
    }

    private static void addKeywordFilter(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates,
            String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String searchTerm = createSearchTerm(keyword);

        Predicate namePredicate = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                searchTerm,
                '\\'
        );

        Predicate descriptionPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("description")),
                searchTerm,
                '\\'
        );

        Predicate categoryPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("category").get("name")),
                searchTerm,
                '\\'
        );

        predicates.add(
                criteriaBuilder.or(
                        namePredicate,
                        descriptionPredicate,
                        categoryPredicate
                )
        );
    }

    private static void addCategoryFilter(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates,
            String category) {

        if (category == null || category.isBlank()) {
            return;
        }

        predicates.add(
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                root.get("category").get("name")
                        ),
                        category.trim().toLowerCase()
                )
        );
    }

    private static void addPriceFilter(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        if (minPrice != null) {
            predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.get("price"),
                            minPrice
                    )
            );
        }

        if (maxPrice != null) {
            predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(
                            root.get("price"),
                            maxPrice
                    )
            );
        }
    }

    private static void addVariantFilter(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates,
            String size,
            String color,
            Boolean inStock) {

        boolean hasSizeFilter = size != null && !size.isBlank();
        boolean hasColorFilter = color != null && !color.isBlank();
        boolean hasVariantFilter =
                hasSizeFilter
                        || hasColorFilter
                        || inStock != null;

        if (!hasVariantFilter) {
            return;
        }

        if (
                Boolean.FALSE.equals(inStock)
                        && !hasSizeFilter
                        && !hasColorFilter
        ) {
            addOutOfStockProductFilter(
                    root,
                    query,
                    criteriaBuilder,
                    predicates
            );
            return;
        }

        Subquery<Integer> subquery = query.subquery(Integer.class);

        jakarta.persistence.criteria.Root<ProductVariant> variant =
                subquery.from(ProductVariant.class);

        List<Predicate> matches = new ArrayList<>();

        matches.add(
                criteriaBuilder.equal(
                        variant.get("product").get("id"),
                        root.get("id")
                )
        );

        addSizeFilter(
                variant,
                criteriaBuilder,
                matches,
                size
        );

        addColorFilter(
                variant,
                criteriaBuilder,
                matches,
                color
        );

        addStockFilter(
                variant,
                criteriaBuilder,
                matches,
                inStock
        );

        subquery
                .select(criteriaBuilder.literal(1))
                .where(matches.toArray(Predicate[]::new));

        predicates.add(
                criteriaBuilder.exists(subquery)
        );
    }

    private static void addOutOfStockProductFilter(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates) {

        Subquery<Integer> availableVariantQuery =
                query.subquery(Integer.class);

        jakarta.persistence.criteria.Root<ProductVariant> variant =
                availableVariantQuery.from(ProductVariant.class);

        availableVariantQuery
                .select(criteriaBuilder.literal(1))
                .where(
                        criteriaBuilder.equal(
                                variant.get("product").get("id"),
                                root.get("id")
                        ),
                        criteriaBuilder.greaterThan(
                                variant.get("stockQuantity"),
                                0
                        )
                );

        predicates.add(
                criteriaBuilder.not(
                        criteriaBuilder.exists(availableVariantQuery)
                )
        );
    }

    private static void addSizeFilter(
            jakarta.persistence.criteria.Root<ProductVariant> variant,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> matches,
            String size) {

        if (size == null || size.isBlank()) {
            return;
        }

        matches.add(
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                variant.get("size")
                        ),
                        size.trim().toLowerCase()
                )
        );
    }

    private static void addColorFilter(
            jakarta.persistence.criteria.Root<ProductVariant> variant,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> matches,
            String color) {

        if (color == null || color.isBlank()) {
            return;
        }

        matches.add(
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                variant.get("color")
                        ),
                        color.trim().toLowerCase()
                )
        );
    }

    private static void addStockFilter(
            jakarta.persistence.criteria.Root<ProductVariant> variant,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> matches,
            Boolean inStock) {

        if (inStock == null) {
            return;
        }

        if (inStock) {
            matches.add(
                    criteriaBuilder.greaterThan(
                            variant.get("stockQuantity"),
                            0
                    )
            );
            return;
        }

        matches.add(
                criteriaBuilder.equal(
                        variant.get("stockQuantity"),
                        0
                )
        );
    }

    private static String createSearchTerm(String keyword) {
        return "%"
                + keyword
                    .trim()
                    .toLowerCase()
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                + "%";
    }
}
