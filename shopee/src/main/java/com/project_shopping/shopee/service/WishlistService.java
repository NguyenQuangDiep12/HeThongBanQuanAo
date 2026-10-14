package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.ProductResponse;
import com.project_shopping.shopee.dto.ApiDtos.VariantResponse;
import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.model.WishlistItem;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.UserRepository;
import com.project_shopping.shopee.repository.WishlistItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WishlistService {
    private final WishlistItemRepository items;
    private final UserRepository users;
    private final ProductRepository products;

    public WishlistService(WishlistItemRepository items, UserRepository users, ProductRepository products) {
        this.items = items;
        this.users = users;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> get(String email) {
        return items.findByUser_EmailAndProduct_StatusAndProduct_Category_ActiveTrueOrderByCreatedAtDesc(
                        email, ProductStatus.ACTIVE)
                .stream().map(item -> toResponse(item.getProduct())).toList();
    }

    @Transactional
    public List<ProductResponse> add(String email, Long productId) {
        if (!items.existsByUser_EmailAndProduct_Id(email, productId)) {
            User user = users.findByEmailIgnoreCase(email).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
            Product product = products.findWithVariantsByIdAndStatusAndCategory_ActiveTrue(productId, ProductStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
            items.save(new WishlistItem(user, product));
        }
        return get(email);
    }

    @Transactional
    public void remove(String email, Long productId) {
        items.findByUser_EmailAndProduct_Id(email, productId).ifPresent(items::delete);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getCategory().getId(),
                product.getCategory().getName(), product.getDescription(), product.getPrice(),
                product.getImageUrl(), product.getStatus(), product.getCreatedAt(),
                product.getVariants().stream().map(v -> new VariantResponse(v.getId(), v.getSize(), v.getColor(),
                        v.getStockQuantity(), v.getPrice())).toList());
    }
}
