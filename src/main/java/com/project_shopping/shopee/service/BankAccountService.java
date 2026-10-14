package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.BankDtos.BankAccountRequest;
import com.project_shopping.shopee.dto.BankDtos.BankAccountResponse;
import com.project_shopping.shopee.model.BankAccount;
import com.project_shopping.shopee.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccounts;

    public BankAccountService(BankAccountRepository bankAccounts) {
        this.bankAccounts = bankAccounts;
    }

    @Transactional
    public BankAccountResponse saveActiveAccount(BankAccountRequest request) {
        bankAccounts.findAllForUpdate().forEach(account -> account.setActive(false));

        BankAccount account = new BankAccount(
                request.bankCode().trim().toUpperCase(Locale.ROOT),
                request.bankName().trim(),
                request.accountNo().trim(),
                request.accountName().trim().toUpperCase(Locale.ROOT),
                request.qrTemplate()
        );
        return toResponse(bankAccounts.save(account));
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listAccounts() {
        return bankAccounts.findAll().stream().map(this::toResponse).toList();
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(), account.getBankCode(), account.getBankName(),
                account.getAccountNo(), account.getAccountName(), account.getQrTemplate(), account.isActive()
        );
    }
}
