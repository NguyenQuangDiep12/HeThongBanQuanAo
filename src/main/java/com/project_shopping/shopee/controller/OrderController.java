package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.CheckoutRequest;
import com.project_shopping.shopee.dto.ApiDtos.OrderResponse;
import com.project_shopping.shopee.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CheckoutRequest request) {

        return orderService.checkout(
                user.getUsername(),
                request
        );
    }

    @GetMapping
    public List<OrderResponse> getMyOrders(
            @AuthenticationPrincipal UserDetails user) {

        return orderService.myOrders(
                user.getUsername()
        );
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderDetail(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        return orderService.myOrder(
                user.getUsername(),
                id
        );
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        return orderService.cancelByCustomer(
                user.getUsername(),
                id
        );
    }

    @PutMapping("/{id}/confirm-receipt")
    public OrderResponse confirmReceipt(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        return orderService.confirmReceipt(
                user.getUsername(),
                id
        );
    }
}