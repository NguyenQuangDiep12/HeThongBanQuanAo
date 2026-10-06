package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.variant",
                    "items.variant.product"
            }
    )
    Optional<Cart> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.variant",
                    "items.variant.product"
            }
    )
    @Query("""
            SELECT c
            FROM Cart c
            WHERE c.user.id = :userId
            """)
    Optional<Cart> findByUserIdForUpdate(
            @Param("userId") Long userId
    );
}