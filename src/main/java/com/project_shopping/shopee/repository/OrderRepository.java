package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.CustomerOrder;
import com.project_shopping.shopee.model.enums.OrderStatus;
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

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END
            FROM CustomerOrder o JOIN o.items item
            WHERE o.user.id = :userId AND o.status = :status
              AND item.variant.product.id = :productId
            """)
    boolean hasPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId, @Param("status") OrderStatus status);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status = :status")
    java.math.BigDecimal sumRevenueByStatus(@Param("status") OrderStatus status);

    @Query("""
            SELECT o.status, COUNT(o)
            FROM CustomerOrder o
            GROUP BY o.status
            ORDER BY o.status
            """)
    List<Object[]> summarizeOrdersByStatus();

    @Query("""
            SELECT oi.variant.product.id,
                   COALESCE(oi.productNameSnapshot, oi.variant.product.name),
                   SUM(oi.quantity),
                   SUM(oi.subTotal)
            FROM OrderItem oi
            WHERE oi.order.status = :status
            GROUP BY oi.variant.product.id, COALESCE(oi.productNameSnapshot, oi.variant.product.name)
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingProducts(@Param("status") OrderStatus status, org.springframework.data.domain.Pageable pageable);

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
