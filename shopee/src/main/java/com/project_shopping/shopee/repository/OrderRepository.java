package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    // ==================== CUSTOMER ====================

    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.variant",
                    "items.variant.product",
                    "payment"
            }
    )
    List<CustomerOrder> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.variant",
                    "items.variant.product",
                    "payment"
            }
    )
    Optional<CustomerOrder> findByIdAndUserId(
            Long id,
            Long userId
    );

    // ==================== ADMIN ====================

    @EntityGraph(
            attributePaths = {
                    "user",
                    "items",
                    "items.variant",
                    "items.variant.product",
                    "payment"
            }
    )
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(
            attributePaths = {
                    "user",
                    "items",
                    "items.variant",
                    "items.variant.product",
                    "payment"
            }
    )
    Optional<CustomerOrder> findDetailedById(
            Long id
    );

    // ==================== CHECKOUT / UPDATE ====================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM CustomerOrder o
            WHERE o.id = :id
            """)
    Optional<CustomerOrder> findByIdForUpdate(
            @Param("id") Long id
    );
}