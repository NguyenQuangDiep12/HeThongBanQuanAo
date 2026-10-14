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
import com.project_shopping.shopee.repository.PaymentTransactionRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.VariantRepository;
import com.project_shopping.shopee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
                        .content("""
                                {"fullName":"Weak Password","email":"weak-%s@example.test","password":"1234567"}
                                """.formatted(suffix)))
                .andExpect(status().isBadRequest());

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
        storedLine.setQuantity(-1); // Simulate a corrupt legacy row; the API must still reject checkout.
        carts.save(buyerCart);
        String checkout = """
                {"shippingAddress":"1 Test Street","phone":"0900000000","paymentMethod":"BANKING"}
                """;
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
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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
        PaymentTransaction pendingTransaction = transactions.findByOrderId(paidBankingOrderId).orElseThrow();
        mvc.perform(put("/api/admin/payment-transactions/{id}/confirm", pendingTransaction.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        assertThat(transactions.findById(pendingTransaction.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentTransaction.TransactionStatus.CONFIRMED);
        assertThat(orders.findById(paidBankingOrderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
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
