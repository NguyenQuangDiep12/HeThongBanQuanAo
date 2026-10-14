package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();
    Optional<PaymentTransaction> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT tx.order.id FROM PaymentTransaction tx WHERE tx.id = :id")
    Optional<Long> findOrderIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tx FROM PaymentTransaction tx WHERE tx.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tx FROM PaymentTransaction tx WHERE tx.order.id = :orderId")
    List<PaymentTransaction> findAllByOrderIdForUpdate(@Param("orderId") Long orderId);
}
