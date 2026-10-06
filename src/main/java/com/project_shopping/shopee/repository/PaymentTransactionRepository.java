package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();
    Optional<PaymentTransaction> findByOrderId(Long orderId);
    Optional<PaymentTransaction> findByTransferMemo(String transferMemo);
}
