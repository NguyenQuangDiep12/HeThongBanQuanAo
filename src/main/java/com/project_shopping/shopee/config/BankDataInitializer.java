package com.project_shopping.shopee.config;

import com.project_shopping.shopee.model.BankAccount;
import com.project_shopping.shopee.repository.BankAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BankDataInitializer implements CommandLineRunner {

    private final BankAccountRepository bankAccountRepository;

    public BankDataInitializer(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    public void run(String... args) {
        if (bankAccountRepository.count() == 0) {
            BankAccount defaultBank = new BankAccount(
                    "MB",
                    "Ngân hàng TMCP Quân Đội (MBBank)",
                    "0987654321",
                    "BLUE THREAD STORE",
                    "compact"
            );
            bankAccountRepository.save(defaultBank);
        }
    }
}
