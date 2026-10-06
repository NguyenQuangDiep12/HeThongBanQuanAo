package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.ReviewRequest;
import com.project_shopping.shopee.dto.ApiDtos.ReviewResponse;
import com.project_shopping.shopee.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {
    private final ReviewService reviews;
    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long productId) { return reviews.list(productId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal UserDetails user, @PathVariable Long productId,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviews.create(user.getUsername(), productId, request);
    }
}
