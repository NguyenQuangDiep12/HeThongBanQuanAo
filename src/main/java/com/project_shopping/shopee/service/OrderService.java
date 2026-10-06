package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.*;
import com.project_shopping.shopee.model.*;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final CartRepository carts;
    private final VariantRepository variants;
    private final NotificationRepository notifications;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public OrderService(OrderRepository orders, UserRepository users, CartRepository carts,
                        VariantRepository variants, NotificationRepository notifications,
                        PaymentTransactionService paymentTransactionService,
                        PaymentTransactionRepository paymentTransactionRepository) {
        this.orders = orders;
        this.users = users;
        this.carts = carts;
        this.variants = variants;
        this.notifications = notifications;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public OrderResponse checkout(String email, CheckoutRequest request) {
        User user = user(email);
        Cart cart = carts.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng đang trống."));
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng đang trống.");
        }

        Map<Long, ProductVariant> lockedVariants = new TreeMap<>();
        for (CartItem item : cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getVariant().getId())).toList()) {
            ProductVariant variant = variants.findByIdForUpdate(item.getVariant().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Một phân loại trong giỏ hàng không còn tồn tại. Vui lòng cập nhật giỏ hàng."));
            if (variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng có sản phẩm đã ngừng kinh doanh. Vui lòng cập nhật giỏ hàng.");
            }
            if (item.getQuantity() > variant.getStockQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Số lượng tồn kho không đủ cho một sản phẩm trong giỏ hàng. Vui lòng cập nhật số lượng.");
            }
            lockedVariants.put(variant.getId(), variant);
        }

        BigDecimal total = cart.getItems().stream()
                .map(item -> lockedVariants.get(item.getVariant().getId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CustomerOrder order = new CustomerOrder(user, request.shippingAddress().trim(), request.phone().trim(), total);
        for (CartItem item : cart.getItems()) {
            ProductVariant variant = lockedVariants.get(item.getVariant().getId());
            order.getItems().add(new OrderItem(order, variant, item.getQuantity(), variant.getPrice()));
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
        }
        Payment payment = new Payment(order, request.paymentMethod(), total);
        order.setPayment(payment);
        CustomerOrder saved = orders.save(order);

        if (request.paymentMethod() == com.project_shopping.shopee.model.enums.PaymentMethod.BANKING) {
            paymentTransactionService.createTransactionForOrder(saved);
        }

        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(String email) {
        User user = user(email);
        return orders.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse myOrder(String email, Long id) {
        User user = user(email);
        return orders.findByIdAndUserId(id, user.getId()).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> allOrders() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse adminOrder(Long id) {
        return orders.findDetailedById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
    }

    @Transactional
    public OrderResponse confirm(Long id) {
        CustomerOrder order = lockOrder(id);
        requireStatus(order, OrderStatus.PENDING, "Chỉ đơn hàng đang chờ xác nhận mới có thể được xác nhận.");
        Instant confirmedAt = timestampAfter(order.getCreatedAt());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(confirmedAt);
        notifyCustomer(order, "Đơn hàng đã được xác nhận", "Đơn hàng #" + order.getId() + " đã được xác nhận.");
        return toResponse(order);
    }

    @Transactional
    public OrderResponse startDelivery(Long id) {
        CustomerOrder order = lockOrder(id);
        requireStatus(order, OrderStatus.CONFIRMED, "Chỉ đơn hàng đã xác nhận mới có thể chuyển sang trạng thái đang giao.");
        Instant shippingAt = timestampAfter(order.getConfirmedAt());
        order.setStatus(OrderStatus.SHIPPING);
        order.setShippingAt(shippingAt);
        notifyCustomer(order, "Đơn hàng đang được giao", "Đơn hàng #" + order.getId() + " đang trên đường giao đến bạn.");
        return toResponse(order);
    }

    @Transactional
    public OrderResponse markDelivered(Long id) {
        CustomerOrder order = lockOrder(id);
        requireStatus(order, OrderStatus.SHIPPING, "Chỉ đơn hàng đang giao mới có thể được xác nhận đã giao.");
        Instant deliveredAt = timestampAfter(order.getShippingAt());
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(deliveredAt);
        notifyCustomer(order, "Đơn hàng đã giao", "Đơn hàng #" + order.getId() + " đã được xác nhận giao thành công.");
        return toResponse(order);
    }

    @Transactional
    public OrderResponse confirmReceipt(String email, Long id) {
        User user = user(email);
        CustomerOrder order = lockOrder(id);
        requireOwner(order, user);
        requireStatus(order, OrderStatus.DELIVERED, "Chỉ đơn hàng đã giao mới có thể được xác nhận đã nhận hàng.");
        Instant customerConfirmedAt = timestampAfter(order.getDeliveredAt());
        order.setStatus(OrderStatus.COMPLETED);
        order.setCustomerConfirmedAt(customerConfirmedAt);
        notifyCustomer(order, "Đã xác nhận nhận hàng", "Bạn đã xác nhận nhận hàng cho đơn hàng #" + order.getId() + ".");
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        CustomerOrder order = lockOrder(id);
        return cancelPendingOrder(order);
    }

    @Transactional
    public OrderResponse cancelByCustomer(String email, Long id) {
        User user = user(email);
        CustomerOrder order = lockOrder(id);
        requireOwner(order, user);
        return cancelPendingOrder(order);
    }

    private OrderResponse cancelPendingOrder(CustomerOrder order) {
        requireStatus(order, OrderStatus.PENDING, "Chỉ đơn hàng đang chờ xác nhận mới có thể được hủy.");
        Instant cancelledAt = timestampAfter(order.getCreatedAt());
        restoreInventory(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(cancelledAt);
        notifyCustomer(order, "Đơn hàng đã hủy", "Đơn hàng #" + order.getId() + " đã được hủy.");
        return toResponse(order);
    }

    private void restoreInventory(CustomerOrder order) {
        Map<Long, ProductVariant> lockedVariants = new TreeMap<>();
        for (OrderItem line : order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getVariant().getId())).toList()) {
            ProductVariant variant = variants.findByIdForUpdate(line.getVariant().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Một phân loại trong đơn hàng không còn tồn tại. Không thể khôi phục tồn kho."));
            lockedVariants.put(variant.getId(), variant);
        }
        for (OrderItem line : order.getItems()) {
            ProductVariant variant = lockedVariants.get(line.getVariant().getId());
            variant.setStockQuantity(Math.addExact(variant.getStockQuantity(), line.getQuantity()));
        }
    }

    private CustomerOrder lockOrder(Long id) {
        return orders.findByIdForUpdate(id).map(order -> orders.findDetailedById(id).orElse(order))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
    }

    private void requireOwner(CustomerOrder order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
        }
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected, String message) {
        if (order.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private Instant timestampAfter(Instant previous) {
        Instant now = Instant.now();
        if (previous != null && now.isBefore(previous)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Thời gian hệ thống không hợp lệ để cập nhật trạng thái đơn hàng.");
        }
        return now;
    }

    private void notifyCustomer(CustomerOrder order, String title, String message) {
        notifications.save(new Notification(order.getUser(), order, title, message));
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản người dùng."));
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderLineResponse> lines = order.getItems().stream()
                .map(item -> new OrderLineResponse(item.getId(), item.getVariant().getId(),
                        item.getVariant().getProduct().getId(), snapshotOr(item.getProductNameSnapshot(), item.getVariant().getProduct().getName()),
                        snapshotOr(item.getSizeSnapshot(), item.getVariant().getSize()),
                        snapshotOr(item.getColorSnapshot(), item.getVariant().getColor()), item.getQuantity(),
                        item.getUnitPrice(), item.getSubTotal()))
                .toList();
        Payment payment = order.getPayment();
        String transferMemo = null;
        if (payment != null && payment.getPaymentMethod() == com.project_shopping.shopee.model.enums.PaymentMethod.BANKING) {
            transferMemo = paymentTransactionRepository.findByOrderId(order.getId())
                    .map(com.project_shopping.shopee.model.PaymentTransaction::getTransferMemo)
                    .orElse(null);
        }
        PaymentResponse paymentResponse = payment == null ? null
                : new PaymentResponse(payment.getPaymentMethod(), payment.getPaymentStatus(),
                payment.getAmount(), payment.getPaidAt(), transferMemo);
        return new OrderResponse(order.getId(), order.getUser().getId(), order.getShippingAddress(),
                order.getPhone(), order.getTotalAmount(), order.getStatus(), order.getCreatedAt(),
                order.getConfirmedAt(), order.getShippingAt(), order.getDeliveredAt(),
                order.getCustomerConfirmedAt(), order.getCancelledAt(), lines, paymentResponse);
    }

    private String snapshotOr(String snapshot, String currentValue) {
        return snapshot == null ? currentValue : snapshot;
    }
}
