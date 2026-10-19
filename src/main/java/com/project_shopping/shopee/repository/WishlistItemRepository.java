package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.WishlistItem;
import com.project_shopping.shopee.model.enums.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    @EntityGraph(attributePaths = {"product", "product.variants", "product.category"})
    List<WishlistItem> findByUser_EmailAndProduct_StatusAndProduct_Category_ActiveTrueOrderByCreatedAtDesc(
            String email, ProductStatus status);

    boolean existsByUser_EmailAndProduct_Id(String email, Long productId);

    Optional<WishlistItem> findByUser_EmailAndProduct_Id(String email, Long productId);
}
