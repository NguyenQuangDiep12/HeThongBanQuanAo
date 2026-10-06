package com.project_shopping.shopee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_no", nullable = false, length = 50)
    private String accountNo;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Column(name = "qr_template", length = 20)
    private String qrTemplate = "compact";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BankAccount(String bankCode, String bankName, String accountNo, String accountName, String qrTemplate) {
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.accountNo = accountNo;
        this.accountName = accountName;
        this.qrTemplate = qrTemplate != null ? qrTemplate : "compact";
    }
}
