package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.BankDtos.PaymentTransactionResponse;
import com.project_shopping.shopee.model.BankAccount;
import com.project_shopping.shopee.model.CustomerOrder;
import com.project_shopping.shopee.model.PaymentTransaction;
import com.project_shopping.shopee.model.PaymentTransaction.TransactionStatus;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.model.enums.PaymentStatus;
import com.project_shopping.shopee.repository.BankAccountRepository;
import com.project_shopping.shopee.repository.OrderRepository;
import com.project_shopping.shopee.repository.PaymentTransactionRepository;
import com.project_shopping.shopee.util.PaymentMemoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final OrderRepository orderRepository;

    public PaymentTransactionService(PaymentTransactionRepository transactionRepository,
                                     BankAccountRepository bankAccountRepository,
                                     OrderRepository orderRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Tự động sinh Mã Chuyển Khoản khi khách đặt hàng:
     * Cú pháp: [Tên mẫu áo bỏ dấu] + [Mã ngẫu nhiên] + [Số tiền]
     * Ví dụ: AOSOMILINEN K92F 350000
     */
    @Transactional
    public PaymentTransaction createTransactionForOrder(CustomerOrder order) {
        String rawProductName = (order.getItems() == null || order.getItems().isEmpty())
                ? "AO"
                : (order.getItems().get(0).getProductNameSnapshot() != null
                    ? order.getItems().get(0).getProductNameSnapshot()
                    : order.getItems().get(0).getVariant().getProduct().getName());

        String cleanName = PaymentMemoUtil.stripAccents(rawProductName);
        if (cleanName.length() > 12) {
            cleanName = cleanName.substring(0, 12);
        }
        if (order.getItems() != null && order.getItems().size() > 1) {
            cleanName += "PLUS";
        }

        String randomCode = PaymentMemoUtil.generateRandomCode(4);
        long amountInt = order.getTotalAmount().longValue();
        String transferMemo = String.format("%s %s %d", cleanName, randomCode, amountInt);

        BankAccount activeBank = bankAccountRepository.findFirstByActiveTrueOrderByIdDesc().orElse(null);
        String bankAccountNo = activeBank != null ? activeBank.getAccountNo() : "N/A";
        String orderCode = "BT-" + String.format("%05d", order.getId());

        PaymentTransaction tx = new PaymentTransaction(
                order, orderCode, cleanName, randomCode, transferMemo, order.getTotalAmount(), bankAccountNo
        );
        return transactionRepository.save(tx);
    }

    /**
     * Admin duyệt xác nhận đã khớp tiền từ Univer Dashboard Sheet
     */
    @Transactional
    public PaymentTransactionResponse confirmPaymentByAdmin(Long transactionId) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch."));

        if (tx.getStatus() == TransactionStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giao dịch này đã được xác nhận trước đó.");
        }

        tx.setStatus(TransactionStatus.CONFIRMED);
        tx.setConfirmedAt(Instant.now());

        // Đồng bộ đổi trạng thái đơn hàng sang CONFIRMED và Payment sang PAID
        CustomerOrder order = tx.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        if (order.getPayment() != null) {
            order.getPayment().setPaymentStatus(PaymentStatus.PAID);
            order.getPayment().setPaidAt(Instant.now());
        }

        orderRepository.save(order);
        PaymentTransaction updated = transactionRepository.save(tx);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getAllTransactionsForUniver() {
        return transactionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        return new PaymentTransactionResponse(
                tx.getId(), tx.getOrder().getId(), tx.getOrderCode(), tx.getProductSampleName(),
                tx.getRandomCode(), tx.getTransferMemo(), tx.getAmount(), tx.getBankAccountNo(),
                tx.getStatus(), tx.getCreatedAt(), tx.getConfirmedAt()
        );
    }
}
