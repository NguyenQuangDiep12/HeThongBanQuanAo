package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.CategoryRequest;
import com.project_shopping.shopee.dto.ApiDtos.CategoryResponse;
import com.project_shopping.shopee.model.Category;
import com.project_shopping.shopee.repository.CategoryRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(
            CategoryRepository categories,
            ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAdmin() {
        return categories.findAdminRows()
                .stream()
                .map(row -> new CategoryResponse(
                        row.getId(),
                        row.getName(),
                        row.getDescription(),
                        row.getActive(),
                        row.getCreatedAt(),
                        row.getProductCount()))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Danh mục đã tồn tại");
        }

        Category category = new Category(name, normalizeDescription(request.description()));
        category.setActive(request.active() == null || request.active());
        return toResponse(categories.save(category), 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy danh mục"));
        String name = request.name().trim();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên danh mục đã được sử dụng");
        }

        category.setName(name);
        category.setDescription(normalizeDescription(request.description()));
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return toResponse(category, products.countByCategory_Id(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy danh mục"));
        if (products.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa danh mục đang có sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước.");
        }
        categories.delete(category);
    }

    @Transactional(readOnly = true)
    public Category requireActive(Long id) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Vui lòng chọn một danh mục hợp lệ"));
        if (!category.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Danh mục đã ngừng hoạt động");
        }
        return category;
    }

    private CategoryResponse toResponse(Category category, long productCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                productCount);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
