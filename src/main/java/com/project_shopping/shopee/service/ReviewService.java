package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.ReviewRequest;
import com.project_shopping.shopee.dto.ApiDtos.ReviewResponse;
import com.project_shopping.shopee.model.ProductReview;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.model.enums.Role;
import com.project_shopping.shopee.repository.OrderRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.ProductReviewRepository;
import com.project_shopping.shopee.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReviewService {
    private final ProductReviewRepository reviews;
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;

    public ReviewService(ProductReviewRepository reviews, ProductRepository products, UserRepository users, OrderRepository orders) {
        this.reviews = reviews;
        this.products = products;
        this.users = users;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long productId) {
        if (!products.existsById(productId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm.");
        return reviews.findByProductIdOrderByCreatedAtDesc(productId).stream().map(this::response).toList();
    }

    @Transactional
    public ReviewResponse create(String email, Long productId, ReviewRequest request) {
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản người dùng."));
        if (user.getRole() != Role.USER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ khách hàng mới có thể đánh giá sản phẩm.");
        var product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
        if (!orders.hasPurchasedProduct(user.getId(), productId, OrderStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ khách hàng đã nhận hàng mới có thể đánh giá sản phẩm.");
        }
        if (reviews.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã đánh giá sản phẩm này.");
        }
        return response(reviews.save(new ProductReview(user, product, request.rating(), request.comment())));
    }

    private ReviewResponse response(ProductReview review) {
        return new ReviewResponse(review.getId(), review.getProduct().getId(), review.getUser().getId(),
                review.getUser().getFullName(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
