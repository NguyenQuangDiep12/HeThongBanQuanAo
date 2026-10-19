package com.project_shopping.shopee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_shopping.shopee.config.ProductCatalogSeeder;
import com.project_shopping.shopee.model.Category;
import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.repository.CategoryRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.VariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.bootstrap.product-catalog.enabled=true")
class ProductCatalogSeederTests {

    @Autowired private ProductCatalogSeeder seeder;
    @Autowired private ProductRepository products;
    @Autowired private VariantRepository variants;
    @Autowired private CategoryRepository categories;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void importsAllRowsOnceAndPublishesThemThroughPublicCatalogApi() throws Exception {
        assertThat(products.count()).isEqualTo(350);
        assertThat(variants.count()).isEqualTo(350);
        assertThat(categories.findAllByOrderByNameAsc()).hasSize(7)
                .allSatisfy(category -> assertThat(products.countByCategory_Id(category.getId()))
                        .isEqualTo(50));

        Category clothing = categories.findByNameIgnoreCase("Quần áo").orElseThrow();
        Product firstItem = products
                .findWithVariantsByNameIgnoreCaseAndCategory_Id(
                        "Áo thun basic 01", clothing.getId())
                .orElseThrow();
        assertThat(firstItem.getPrice()).isEqualByComparingTo(new BigDecimal("99000"));
        assertThat(firstItem.getImageUrl()).startsWith("https://images.unsplash.com/");
        assertThat(firstItem.getDescription()).contains("Shopee");
        assertThat(firstItem.getVariants()).singleElement().satisfies(variant -> {
            assertThat(variant.getSize()).isEqualTo("S");
            assertThat(variant.getColor()).isEqualTo("Đen");
            assertThat(variant.getStockQuantity()).isEqualTo(25);
            assertThat(variant.getPrice()).isEqualByComparingTo(new BigDecimal("99000"));
        });

        Long variantId = firstItem.getVariants().get(0).getId();
        firstItem.setPrice(new BigDecimal("111000"));
        firstItem.getVariants().get(0).setPrice(new BigDecimal("111000"));
        firstItem.getVariants().get(0).setStockQuantity(10);
        products.saveAndFlush(firstItem);

        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertThat(products.count()).isEqualTo(350);
        assertThat(variants.count()).isEqualTo(350);
        Product preservedProduct = products.findById(firstItem.getId()).orElseThrow();
        assertThat(preservedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("111000"));
        assertThat(variants.findById(variantId).orElseThrow().getPrice())
                .isEqualByComparingTo(new BigDecimal("111000"));
        assertThat(variants.findById(variantId).orElseThrow().getStockQuantity()).isEqualTo(10);

        mvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(350))
                .andExpect(jsonPath("$.items.length()").value(100));

        assertThat(categories.findAllByOrderByNameAsc())
                .extracting(Category::getName)
                .contains("Quần áo", "Giày dép", "Túi xách, balo", "Phụ kiện thời trang",
                        "Đồ thể thao", "Quần áo trẻ em", "Ví, thắt lưng");

        String email = "catalog-buyer-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Catalog Buyer","email":"%s","password":"catalog-password-1","phone":"0900000001"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        String loginResponse = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"catalog-password-1"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();
        mvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"variantId":%d,"quantity":2}
                                """.formatted(variantId)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"1 Test Street","phone":"0900000001","paymentMethod":"COD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(222000))
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(variants.findById(variantId).orElseThrow().getStockQuantity()).isEqualTo(8);
    }
}
