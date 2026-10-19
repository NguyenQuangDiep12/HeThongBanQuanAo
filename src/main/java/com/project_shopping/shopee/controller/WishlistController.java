package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.ProductResponse;
import com.project_shopping.shopee.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<ProductResponse> getWishlist(@AuthenticationPrincipal UserDetails user) {
        return wishlistService.get(user.getUsername());
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProductResponse> add(@AuthenticationPrincipal UserDetails user, @PathVariable Long productId) {
        return wishlistService.add(user.getUsername(), productId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserDetails user, @PathVariable Long productId) {
        wishlistService.remove(user.getUsername(), productId);
    }
}
