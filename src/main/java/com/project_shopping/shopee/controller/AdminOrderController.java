package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.OrderResponse;
import com.project_shopping.shopee.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> listOrders() {
        return orderService.allOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderDetail(@PathVariable Long id) {
        return orderService.adminOrder(id);
    }

    @PutMapping("/{id}/confirm")
    public OrderResponse confirmOrder(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PutMapping("/{id}/start-delivery")
    public OrderResponse startDelivery(@PathVariable Long id) {
        return orderService.startDelivery(id);
    }

    @PutMapping("/{id}/mark-delivered")
    public OrderResponse markDelivered(@PathVariable Long id) {
        return orderService.markDelivered(id);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}