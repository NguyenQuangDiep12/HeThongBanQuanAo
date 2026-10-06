package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findFirstByActiveTrueOrderByIdDesc();
}
