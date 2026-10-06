package com.project_shopping.shopee.dto;

import com.project_shopping.shopee.model.enums.NotificationType;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.model.enums.PaymentMethod;
import com.project_shopping.shopee.model.enums.PaymentStatus;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.model.enums.Role;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    // ==================== AUTH ====================

    public record RegisterRequest(
            @NotBlank
            @Size(max = 120)
            String fullName,

            @NotBlank
            @Email
            @Size(max = 190)
            String email,

            @NotBlank
            String password,

            @Size(max = 30)
            String phone
    ) {
    }

    public record LoginRequest(
            @NotBlank
            @Email
            String email,

            @NotBlank
            String password
    ) {
    }

    public record AuthResponse(
            String accessToken,
            Long userId,
            Role role
    ) {
    }

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            Role role,
            String phone
    ) {
    }

    // ==================== PRODUCT ====================

    public record ProductRequest(
            @NotBlank
            @Size(max = 180)
            String name,

            @NotNull
            @Positive
            Long categoryId,

            String description,

            @NotNull
            @DecimalMin("0.01")
            BigDecimal price,

            @Size(max = 1000)
            String imageUrl,

            ProductStatus status
    ) {
    }

    public record VariantRequest(
            @NotBlank
            @Size(max = 40)
            String size,

            @NotBlank
            @Size(max = 60)
            String color,

            @NotNull
            @PositiveOrZero
            Integer stockQuantity,

            @NotNull
            @DecimalMin("0.01")
            BigDecimal price
    ) {
    }

    public record VariantResponse(
            Long id,
            String size,
            String color,
            int stockQuantity,
            BigDecimal price
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            Long categoryId,
            String category,
            String description,
            BigDecimal price,
            String imageUrl,
            ProductStatus status,
            Instant createdAt,
            List<VariantResponse> variants
    ) {
    }

    public record CategoryRequest(
            @NotBlank
            @Size(max = 80)
            String name,

            @Size(max = 500)
            String description,

            Boolean active
    ) {
    }

    public record CategoryResponse(
            Long id,
            String name,
            String description,
            boolean active,
            Instant createdAt,
            long productCount
    ) {
    }

    public record ProductPageResponse(
            List<ProductResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }

    public record ProductFilterOptions(
            List<String> categories,
            List<String> sizes,
            List<String> colors,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
    }

    // ==================== ADMIN STATISTICS ====================

    public record AdminStatisticsResponse(
            long totalProducts,
            long activeProducts,
            long inactiveProducts,
            long totalInventory,
            long lowStockProducts,
            long outOfStockProducts,
            long totalOrders,
            long pendingOrders,
            long completedOrders,
            long cancelledOrders,
            long totalCustomers,
            BigDecimal completedRevenue,
            List<CategoryStatistic> productsByCategory,
            List<OrderStatusStatistic> ordersByStatus,
            List<TopProductStatistic> topSellingProducts
    ) {
    }

    public record CategoryStatistic(String category, long productCount, long stockQuantity) {
    }

    public record OrderStatusStatistic(String status, long orderCount) {
    }

    public record TopProductStatistic(Long productId, String productName, long quantitySold, BigDecimal revenue) {
    }

    public record ReviewRequest(
            @NotNull @Positive @Max(5) Integer rating,
            @Size(max = 1000) String comment
    ) {
    }

    public record ReviewResponse(
            Long id,
            Long productId,
            Long userId,
            String customerName,
            int rating,
            String comment,
            Instant createdAt
    ) {
    }

    public record AdminUserResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            Role role,
            boolean active,
            Instant createdAt
    ) {
    }

    public record ProfileUpdateRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone
    ) { }

    // ==================== CART ====================

    public record CartItemRequest(
            @NotNull
            @Positive
            Long variantId,

            @NotNull
            @Positive
            Integer quantity
    ) {
    }

    public record CartQuantityRequest(
            @NotNull
            @Positive
            Integer quantity
    ) {
    }

    public record CartLineResponse(
            Long id,
            Long variantId,
            Long productId,
            String productName,
            String size,
            String color,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subTotal,
            int availableStock
    ) {
    }

    public record CartResponse(
            Long id,
            List<CartLineResponse> items,
            BigDecimal totalAmount
    ) {
    }

    // ==================== ORDER ====================

    public record CheckoutRequest(
            @NotBlank
            @Size(max = 500)
            String shippingAddress,

            @NotBlank
            @Size(max = 30)
            String phone,

            @NotNull
            PaymentMethod paymentMethod
    ) {
    }

    public record OrderLineResponse(
            Long id,
            Long variantId,
            Long productId,
            String productName,
            String size,
            String color,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subTotal
    ) {
    }

    public record PaymentResponse(
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            BigDecimal amount,
            Instant paidAt,
            String transferMemo
    ) {
    }

    public record OrderResponse(
            Long id,
            Long userId,
            String shippingAddress,
            String phone,
            BigDecimal totalAmount,
            OrderStatus status,
            Instant createdAt,
            Instant confirmedAt,
            Instant shippingAt,
            Instant deliveredAt,
            Instant customerConfirmedAt,
            Instant cancelledAt,
            List<OrderLineResponse> items,
            PaymentResponse payment
    ) {
    }

    // ==================== NOTIFICATION ====================

    public record NotificationResponse(
            Long id,
            Long orderId,
            String title,
            String message,
            NotificationType type,
            boolean read,
            Instant createdAt
    ) {
    }

    // ==================== ERROR ====================

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message
    ) {
    }
}
