package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.BankDtos.BankAccountRequest;
import com.project_shopping.shopee.dto.BankDtos.BankAccountResponse;
import com.project_shopping.shopee.dto.BankDtos.PaymentTransactionResponse;
import com.project_shopping.shopee.model.BankAccount;
import com.project_shopping.shopee.repository.BankAccountRepository;
import com.project_shopping.shopee.service.PaymentTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminBankController {

    private final BankAccountRepository bankAccountRepository;
    private final PaymentTransactionService transactionService;

    public AdminBankController(BankAccountRepository bankAccountRepository,
                               PaymentTransactionService transactionService) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionService = transactionService;
    }

    // 1. Admin cấu hình / lưu tài khoản ngân hàng
    @PostMapping("/bank-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse saveBankAccount(@Valid @RequestBody BankAccountRequest request) {
        bankAccountRepository.findAll().forEach(b -> b.setActive(false));

        BankAccount bank = new BankAccount(
                request.bankCode().trim().toUpperCase(),
                request.bankName().trim(),
                request.accountNo().trim(),
                request.accountName().trim().toUpperCase(),
                request.qrTemplate()
        );
        BankAccount saved = bankAccountRepository.save(bank);
        return new BankAccountResponse(
                saved.getId(), saved.getBankCode(), saved.getBankName(),
                saved.getAccountNo(), saved.getAccountName(), saved.getQrTemplate(), saved.isActive()
        );
    }

    // 2. Lấy danh sách ngân hàng
    @GetMapping("/bank-accounts")
    public List<BankAccountResponse> listBankAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(b -> new BankAccountResponse(b.getId(), b.getBankCode(), b.getBankName(),
                        b.getAccountNo(), b.getAccountName(), b.getQrTemplate(), b.isActive()))
                .toList();
    }

    // 3. Lấy toàn bộ lịch sử thanh toán phục vụ đổ dữ liệu cho Univer Sheet Engine
    @GetMapping("/payment-transactions")
    public List<PaymentTransactionResponse> listTransactionsForUniver() {
        return transactionService.getAllTransactionsForUniver();
    }

    // 4. Admin bấm duyệt khớp tiền từ Univer Sheet
    @PutMapping("/payment-transactions/{id}/confirm")
    public PaymentTransactionResponse confirmPayment(@PathVariable Long id) {
        return transactionService.confirmPaymentByAdmin(id);
    }
}
