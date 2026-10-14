package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findFirstByActiveTrueOrderByIdDesc();

    /** Serialize account replacement against the existing account rows. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM BankAccount account")
    List<BankAccount> findAllForUpdate();
}
