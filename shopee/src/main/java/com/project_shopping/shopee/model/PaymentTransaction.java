package com.project_shopping.shopee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    public enum TransactionStatus {
        PENDING,      // Chờ khách chuyển khoản / Admin đối soát
        CONFIRMED,    // Đã khớp tiền từ Univer
        REJECTED      // Từ chối / Hủy
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "order_code", nullable = false, length = 50)
    private String orderCode;

    @Column(name = "product_sample_name", length = 100)
    private String productSampleName;

    @Column(name = "random_code", nullable = false, length = 20)
    private String randomCode;

    @Column(name = "transfer_memo", nullable = false, length = 100)
    private String transferMemo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_account_no", length = 50)
    private String bankAccountNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "univer_row_id", length = 50)
    private String univerRowId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public PaymentTransaction(CustomerOrder order, String orderCode, String productSampleName,
                              String randomCode, String transferMemo, BigDecimal amount, String bankAccountNo) {
        this.order = order;
        this.orderCode = orderCode;
        this.productSampleName = productSampleName;
        this.randomCode = randomCode;
        this.transferMemo = transferMemo;
        this.amount = amount;
        this.bankAccountNo = bankAccountNo;
    }
}
