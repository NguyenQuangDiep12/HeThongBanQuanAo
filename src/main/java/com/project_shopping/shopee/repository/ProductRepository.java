package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.model.enums.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    // ==================== PRODUCT ====================

    @EntityGraph(attributePaths = {"variants", "category"})
    List<Product> findByStatus(
            ProductStatus status
    );

    @EntityGraph(attributePaths = {"variants", "category"})
    Optional<Product> findWithVariantsByIdAndStatusAndCategory_ActiveTrue(
            Long id,
            ProductStatus status
    );

    @EntityGraph(attributePaths = {"variants", "category"})
    Optional<Product> findWithVariantsById(
            Long id
    );

    @EntityGraph(attributePaths = {"variants", "category"})
    Optional<Product> findWithVariantsByNameIgnoreCaseAndCategory_Id(
            String name,
            Long categoryId
    );

    @EntityGraph(attributePaths = {"variants", "category"})
    List<Product> findAllByOrderByCreatedAtDesc();

    // ==================== RELATED PRODUCTS ====================

    @EntityGraph(attributePaths = {"variants", "category"})
    List<Product> findByStatusAndCategory_IdAndCategory_ActiveTrueAndIdNotOrderByCreatedAtDesc(
            ProductStatus status,
            Long categoryId,
            Long id
    );

    long countByCategory_Id(Long categoryId);

    boolean existsByCategory_Id(Long categoryId);

    long countByStatus(ProductStatus status);

    @Query("SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v")
    long totalInventory();

    @Query("""
            SELECT COUNT(p)
            FROM Product p
            WHERE p.status = :status
              AND (SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.product = p) BETWEEN 1 AND :threshold
            """)
    long countLowStockProducts(@Param("status") ProductStatus status, @Param("threshold") int threshold);

    @Query("""
            SELECT COUNT(p)
            FROM Product p
            WHERE p.status = :status
              AND NOT EXISTS (SELECT v.id FROM ProductVariant v WHERE v.product = p AND v.stockQuantity > 0)
            """)
    long countOutOfStockProducts(@Param("status") ProductStatus status);

    @Query("""
            SELECT p.category.name, COUNT(DISTINCT p), COALESCE(SUM(v.stockQuantity), 0)
            FROM Product p LEFT JOIN p.variants v
            GROUP BY p.category.id, p.category.name
            ORDER BY p.category.name
            """)
    List<Object[]> summarizeProductsByCategory();

    // ==================== FILTER OPTIONS ====================

    @Query("""
            SELECT DISTINCT p.category.name
            FROM Product p
            WHERE p.status = :status
              AND p.category.active = TRUE
            ORDER BY p.category.name
            """)
    List<String> findDistinctCategories(
            @Param("status") ProductStatus status
    );

    @Query("""
            SELECT DISTINCT v.size
            FROM ProductVariant v
            WHERE v.product.status = :status
              AND v.product.category.active = TRUE
              AND v.stockQuantity > 0
            ORDER BY v.size
            """)
    List<String> findAvailableSizes(
            @Param("status") ProductStatus status
    );

    @Query("""
            SELECT DISTINCT v.color
            FROM ProductVariant v
            WHERE v.product.status = :status
              AND v.product.category.active = TRUE
              AND v.stockQuantity > 0
            ORDER BY v.color
            """)
    List<String> findAvailableColors(
            @Param("status") ProductStatus status
    );

    @Query("""
            SELECT MIN(p.price)
            FROM Product p
            WHERE p.status = :status
              AND p.category.active = TRUE
              AND EXISTS (
                  SELECT v.id
                  FROM ProductVariant v
                  WHERE v.product = p
                    AND v.stockQuantity > 0
              )
            """)
    BigDecimal findMinAvailablePrice(
            @Param("status") ProductStatus status
    );

    @Query("""
            SELECT MAX(p.price)
            FROM Product p
            WHERE p.status = :status
              AND p.category.active = TRUE
              AND EXISTS (
                  SELECT v.id
                  FROM ProductVariant v
                  WHERE v.product = p
                    AND v.stockQuantity > 0
              )
            """)
    BigDecimal findMaxAvailablePrice(
            @Param("status") ProductStatus status
    );
}
