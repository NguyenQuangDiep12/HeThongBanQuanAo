package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.AdminStatisticsResponse;
import com.project_shopping.shopee.dto.ApiDtos.CategoryStatistic;
import com.project_shopping.shopee.dto.ApiDtos.OrderStatusStatistic;
import com.project_shopping.shopee.dto.ApiDtos.TopProductStatistic;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.model.enums.Role;
import com.project_shopping.shopee.repository.OrderRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminStatisticsService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;

    public AdminStatisticsService(ProductRepository products, OrderRepository orders, UserRepository users) {
        this.products = products;
        this.orders = orders;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse dashboard() {
        long totalProducts = products.count();
        long activeProducts = products.countByStatus(ProductStatus.ACTIVE);
        long totalOrders = orders.count();
        List<CategoryStatistic> byCategory = products.summarizeProductsByCategory().stream()
                .map(row -> new CategoryStatistic((String) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();
        List<OrderStatusStatistic> byStatus = orders.summarizeOrdersByStatus().stream()
                .map(row -> new OrderStatusStatistic(row[0].toString(), ((Number) row[1]).longValue()))
                .toList();
        List<TopProductStatistic> topProducts = orders.findTopSellingProducts(OrderStatus.COMPLETED, PageRequest.of(0, 10)).stream()
                .map(row -> new TopProductStatistic(((Number) row[0]).longValue(), (String) row[1],
                        ((Number) row[2]).longValue(), (BigDecimal) row[3]))
                .toList();

        return new AdminStatisticsResponse(
                totalProducts,
                activeProducts,
                products.countByStatus(ProductStatus.INACTIVE),
                products.totalInventory(),
                products.countLowStockProducts(ProductStatus.ACTIVE, LOW_STOCK_THRESHOLD),
                products.countOutOfStockProducts(ProductStatus.ACTIVE),
                totalOrders,
                orders.countByStatus(OrderStatus.PENDING),
                orders.countByStatus(OrderStatus.COMPLETED),
                orders.countByStatus(OrderStatus.CANCELLED),
                users.countByRole(Role.USER),
                orders.sumRevenueByStatus(OrderStatus.COMPLETED),
                byCategory,
                byStatus,
                topProducts
        );
    }
}
