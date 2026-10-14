package com.project_shopping.shopee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_shopping.shopee.model.Category;
import com.project_shopping.shopee.model.Cart;
import com.project_shopping.shopee.model.CartItem;
import com.project_shopping.shopee.model.CustomerOrder;
import com.project_shopping.shopee.model.PaymentTransaction;
import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.model.ProductVariant;
import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.repository.BankAccountRepository;
import com.project_shopping.shopee.repository.CategoryRepository;
import com.project_shopping.shopee.repository.CartRepository;
import com.project_shopping.shopee.repository.OrderRepository;
import com.project_shopping.shopee.repository.NotificationRepository;
import com.project_shopping.shopee.repository.PaymentTransactionRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.VariantRepository;
import com.project_shopping.shopee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CriticalBusinessFlowTests {

    private static final String CUSTOMER_PASSWORD = "customer-password-1";
    private static final String ADMIN_EMAIL = "admin@ecommerce.test";
    private static final String ADMIN_PASSWORD = "admin-test-password";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categories;
    @Autowired private CartRepository carts;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private VariantRepository variants;
    @Autowired private OrderRepository orders;
    @Autowired private PaymentTransactionRepository transactions;
    @Autowired private BankAccountRepository bankAccounts;
    @Autowired private NotificationRepository notifications;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void preventsQuantityOverflowAndSerializesBankingCancellationAndOrderLifecycle() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String customerEmail = "buyer-" + suffix + "@example.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Buyer","email":"%s","password":"%s","phone":"0900000000"}
                                """.formatted(customerEmail, CUSTOMER_PASSWORD)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "fr")
                        .content("""
                                {"fullName":"Weak Password","email":"weak-%s@example.test","password":"1234567"}
                                """.formatted(suffix)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("mật khẩu phải có từ 8 đến 72 ký tự."));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(customerEmail, "x".repeat(73))))
                .andExpect(status().isUnauthorized());

        String customerToken = login(customerEmail, CUSTOMER_PASSWORD);
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(get("/api/route-that-does-not-exist")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/register")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/auth/register")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        Category category = categories.save(new Category("Test " + suffix, "Unit test fixture"));
        Product product = new Product("Test product " + suffix, "Fixture", new BigDecimal("9000.00"), null);
        product.setCategory(category);
        Product savedProduct = products.save(product);
        ProductVariant variant = variants.save(new ProductVariant(
                savedProduct, "M", "Black", 5, new BigDecimal("2500.00")));

        long productsBeforePriceValidation = products.count();
        mvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Whole price %s","categoryId":%d,"description":"VND fixture","price":199000.00}
                                """.formatted(suffix, category.getId())))
                .andExpect(status().isCreated());
        long productsAfterWholePrice = products.count();
        mvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fractional price %s","categoryId":%d,"description":"VND fixture","price":199000.50}
                                """.formatted(suffix, category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("giá phải là số nguyên VND."));
        assertThat(products.count()).isEqualTo(productsAfterWholePrice);
        assertThat(productsAfterWholePrice).isEqualTo(productsBeforePriceValidation + 1);

        mvc.perform(post("/api/admin/products/{productId}/variants", savedProduct.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"size":"L","color":"Red","stockQuantity":1,"price":199000.00}
                                """))
                .andExpect(status().isCreated());
        long variantsBeforeInvalidPrice = variants.count();
        mvc.perform(post("/api/admin/products/{productId}/variants", savedProduct.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"size":"XL","color":"Blue","stockQuantity":1,"price":199000.50}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("giá phải là số nguyên VND."));
        assertThat(variants.count()).isEqualTo(variantsBeforeInvalidPrice);

        // Re-adding Integer.MAX_VALUE used to wrap the accumulated int quantity negative.
        addCartItem(customerToken, variant.getId(), Integer.MAX_VALUE)
                .andExpect(status().isBadRequest());
        addCartItem(customerToken, variant.getId(), 3)
                .andExpect(status().isCreated());
        addCartItem(customerToken, variant.getId(), Integer.MAX_VALUE)
                .andExpect(status().isBadRequest());
        assertThat(variants.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);

        User buyer = users.findByEmailIgnoreCase(customerEmail).orElseThrow();
        Cart buyerCart = carts.findByUserId(buyer.getId()).orElseThrow();
        CartItem storedLine = buyerCart.getItems().get(0);

        // DECIMAL(14,2) caps the persisted order/subtotal amount at 999,999,999,999.99.
        variant.setPrice(new BigDecimal("9999999999.00"));
        variant.setStockQuantity(101);
        variants.saveAndFlush(variant);
        storedLine.setQuantity(101);
        carts.saveAndFlush(buyerCart);
        String checkout = """
                {"shippingAddress":"1 Test Street","phone":"0900000000","paymentMethod":"BANKING"}
                """;
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tổng tiền đơn hàng vượt quá giới hạn dữ liệu cho phép."));
        assertThat(orders.count()).isZero();
        assertThat(variants.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(101);

        storedLine.setQuantity(3);
        carts.saveAndFlush(buyerCart);
        variant.setPrice(new BigDecimal("2500.00"));
        variant.setStockQuantity(5);
        variants.saveAndFlush(variant);

        storedLine.setQuantity(-1); // Simulate a corrupt legacy row; the API must still reject checkout.
        carts.save(buyerCart);
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout))
                .andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
        assertThat(variants.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        storedLine.setQuantity(3);
        carts.save(buyerCart);

        mvc.perform(get("/api/admin/bank-accounts").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout))
                .andExpect(status().isConflict());
        assertThat(orders.count()).isZero();
        assertThat(variants.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);

        mvc.perform(post("/api/admin/bank-accounts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"TEST","bankName":"Test Bank","accountNo":"1234567890","accountName":"BLUE THREAD TEST","qrTemplate":"compact"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
        mvc.perform(post("/api/admin/bank-accounts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"TEST2","bankName":"Second Test Bank","accountNo":"0987654321","accountName":"BLUE THREAD TEST 2","qrTemplate":"compact"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
        assertThat(bankAccounts.findFirstByActiveTrueOrderByIdDesc()).isPresent();
        assertThat(bankAccounts.findAll().stream().filter(account -> account.isActive()).count()).isEqualTo(1);

        JsonNode bankingOrder = objectMapper.readTree(mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
        long bankingOrderId = bankingOrder.get("id").asLong();
        assertThat(bankingOrder.at("/payment/transferMemo").asText()).contains("BT-");
        assertThat(variants.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);

        CustomerOrder storedBankingOrder = orders.findById(bankingOrderId).orElseThrow();
        transactions.save(new PaymentTransaction(storedBankingOrder,
                "BT-DUP-" + suffix, "Test product", "DUP" + suffix.substring(0, 5),
                "BT-DUP-" + suffix, new BigDecimal("7500.00"), "0987654321"));

        mvc.perform(put("/api/orders/{id}/cancel", bankingOrderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payment.paymentStatus").value("FAILED"));
        var cancelledTransactions = transactions.findAll().stream()
                .filter(transaction -> transaction.getOrder().getId().equals(bankingOrderId))
                .toList();
        assertThat(cancelledTransactions).hasSize(2)
                .allSatisfy(transaction -> assertThat(transaction.getStatus())
                        .isEqualTo(PaymentTransaction.TransactionStatus.REJECTED));
        PaymentTransaction cancelledTransaction = cancelledTransactions.get(0);
        assertThat(variants.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);

        mvc.perform(put("/api/admin/payment-transactions/{id}/confirm", cancelledTransaction.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
        assertThat(orders.findById(bankingOrderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(transactions.findById(cancelledTransaction.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentTransaction.TransactionStatus.REJECTED);

        addCartItem(customerToken, variant.getId(), 1).andExpect(status().isCreated());
        JsonNode paidBankingOrder = objectMapper.readTree(mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
        long paidBankingOrderId = paidBankingOrder.get("id").asLong();
        PaymentTransaction pendingTransaction = transactions.findFirstByOrderIdOrderByCreatedAtDesc(paidBankingOrderId).orElseThrow();
        mvc.perform(put("/api/admin/payment-transactions/{id}/confirm", pendingTransaction.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        assertThat(transactions.findById(pendingTransaction.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentTransaction.TransactionStatus.CONFIRMED);
        assertThat(orders.findById(paidBankingOrderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(notifications.findAllByUserIdOrderByCreatedAtDesc(buyer.getId()))
                .anySatisfy(notification -> assertThat(notification.getTitle())
                        .isEqualTo("Đơn hàng đã được xác nhận"));
        mvc.perform(get("/api/admin/orders/{id}", paidBankingOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.paymentStatus").value("PAID"));

        // Exercise the normal COD path through each available order state transition.
        addCartItem(customerToken, variant.getId(), 2).andExpect(status().isCreated());
        JsonNode codOrder = objectMapper.readTree(mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"1 Test Street","phone":"0900000000","paymentMethod":"COD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        long codOrderId = codOrder.get("id").asLong();

        mvc.perform(put("/api/admin/orders/{id}/confirm", codOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(put("/api/admin/orders/{id}/start-delivery", codOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPING"));
        mvc.perform(put("/api/admin/orders/{id}/mark-delivered", codOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        mvc.perform(put("/api/orders/{id}/confirm-receipt", codOrderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/orders/{id}", codOrderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Historical names for one product must not create duplicate top-selling rows.
        jdbcTemplate.update("""
                UPDATE order_items
                SET quantity = 1, sub_total = 2500.00, product_name_snapshot = ?
                WHERE order_id = ?
                """, "Older product name", codOrderId);
        jdbcTemplate.update("""
                INSERT INTO order_items (
                    order_id, variant_id, product_name_snapshot, size_snapshot, color_snapshot,
                    quantity, unit_price, sub_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, codOrderId, variant.getId(), "Newer product name", "M", "Black",
                1, new BigDecimal("2500.00"), new BigDecimal("2500.00"));

        List<Object[]> topProducts = orders.findTopSellingProducts(
                OrderStatus.COMPLETED, PageRequest.of(0, 10));
        List<Object[]> matchingProductRows = topProducts.stream()
                .filter(row -> ((Number) row[0]).longValue() == savedProduct.getId())
                .toList();
        assertThat(matchingProductRows).hasSize(1);
        assertThat(((Number) matchingProductRows.get(0)[2]).longValue()).isEqualTo(2);
        assertThat((BigDecimal) matchingProductRows.get(0)[3]).isEqualByComparingTo("5000.00");
    }

    private org.springframework.test.web.servlet.ResultActions addCartItem(
            String token, Long variantId, int quantity) throws Exception {
        return mvc.perform(post("/api/cart/items")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":%d,\"quantity\":%d}".formatted(variantId, quantity)));
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
