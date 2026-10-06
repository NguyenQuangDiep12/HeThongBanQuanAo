package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VariantRepository
        extends JpaRepository<ProductVariant, Long> {

    @EntityGraph(attributePaths = "product")
    java.util.List<ProductVariant> findAllByProduct_IdOrderBySizeAscColorAsc(
            Long productId
    );

    boolean existsByProduct_IdAndSizeIgnoreCaseAndColorIgnoreCase(
            Long productId,
            String size,
            String color
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT v
            FROM ProductVariant v
            JOIN FETCH v.product
            WHERE v.id = :id
            """)
    Optional<ProductVariant> findByIdForUpdate(
            @Param("id") Long id
    );
}
