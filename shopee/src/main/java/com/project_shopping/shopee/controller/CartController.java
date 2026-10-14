package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.CartItemRequest;
import com.project_shopping.shopee.dto.ApiDtos.CartQuantityRequest;
import com.project_shopping.shopee.dto.ApiDtos.CartResponse;
import com.project_shopping.shopee.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(
            @AuthenticationPrincipal UserDetails user) {

        return cartService.get(user.getUsername());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CartItemRequest request) {

        return cartService.add(
                user.getUsername(),
                request
        );
    }

    @PutMapping("/items/{id}")
    public CartResponse updateItem(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody CartQuantityRequest request) {

        return cartService.update(
                user.getUsername(),
                id,
                request
        );
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        cartService.delete(
                user.getUsername(),
                id
        );
    }
}