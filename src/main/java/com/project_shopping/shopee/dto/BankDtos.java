package com.project_shopping.shopee.dto;

import com.project_shopping.shopee.model.PaymentTransaction.TransactionStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

public final class BankDtos {

    private BankDtos() {}

    public record BankAccountRequest(
            @NotBlank String bankCode,
            @NotBlank String bankName,
            @NotBlank String accountNo,
            @NotBlank String accountName,
            String qrTemplate
    ) {}

    public record BankAccountResponse(
            Long id,
            String bankCode,
            String bankName,
            String accountNo,
            String accountName,
            String qrTemplate,
            boolean active
    ) {}

    public record PaymentTransactionResponse(
            Long id,
            Long orderId,
            String orderCode,
            String productSampleName,
            String randomCode,
            String transferMemo,
            BigDecimal amount,
            String bankAccountNo,
            TransactionStatus status,
            Instant createdAt,
            Instant confirmedAt
    ) {}
}
