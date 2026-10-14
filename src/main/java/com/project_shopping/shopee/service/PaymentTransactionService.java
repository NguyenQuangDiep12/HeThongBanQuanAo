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

        String randomCode = PaymentMemoUtil.generateRandomCode(8);
        long amountInt;
        try {
            amountInt = order.getTotalAmount().toBigIntegerExact().longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tổng tiền chuyển khoản phải là số nguyên VND."
            );
        }

        BankAccount activeBank = bankAccountRepository.findFirstByActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Chưa cấu hình tài khoản ngân hàng nhận thanh toán."
                ));
        String bankAccountNo = activeBank.getAccountNo();
        String orderCode = "BT-" + String.format("%05d", order.getId());
        String transferMemo = String.format("%s %s %s %d", orderCode, cleanName, randomCode, amountInt);

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
        Long orderId = transactionRepository.findOrderIdById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch."));
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        PaymentTransaction tx = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch."));

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch không ở trạng thái chờ xác nhận.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng không còn ở trạng thái chờ xác nhận.");
        }
        if (order.getPayment() == null
                || order.getPayment().getPaymentMethod() != com.project_shopping.shopee.model.enums.PaymentMethod.BANKING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng không có phương thức thanh toán chuyển khoản.");
        }

        order = orderRepository.findDetailedById(orderId).orElse(order);
        Instant confirmedAt = Instant.now();
        tx.setStatus(TransactionStatus.CONFIRMED);
        tx.setConfirmedAt(confirmedAt);

        // Đồng bộ đổi trạng thái đơn hàng sang CONFIRMED và Payment sang PAID
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(confirmedAt);
        if (order.getPayment() != null) {
            order.getPayment().setPaymentStatus(PaymentStatus.PAID);
            order.getPayment().setPaidAt(confirmedAt);
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
