package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.BankDtos.BankAccountResponse;
import com.project_shopping.shopee.model.BankAccount;
import com.project_shopping.shopee.repository.BankAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bank-accounts")
public class PublicBankController {

    private final BankAccountRepository bankAccountRepository;

    public PublicBankController(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    // Khách hàng lấy thông tin ngân hàng active tại bước Checkout để tạo VietQR
    @GetMapping("/active")
    public BankAccountResponse getActiveBank() {
        BankAccount bank = bankAccountRepository.findFirstByActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa cấu hình tài khoản ngân hàng."));
        return new BankAccountResponse(
                bank.getId(), bank.getBankCode(), bank.getBankName(),
                bank.getAccountNo(), bank.getAccountName(), bank.getQrTemplate(), bank.isActive()
        );
    }
}
