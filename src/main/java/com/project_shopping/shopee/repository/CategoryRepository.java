package com.project_shopping.shopee.repository;

import com.project_shopping.shopee.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT c.id AS id,
                   c.name AS name,
                   c.description AS description,
                   c.active AS active,
                   c.createdAt AS createdAt,
                   COUNT(p.id) AS productCount
            FROM Category c
            LEFT JOIN c.products p
            GROUP BY c.id, c.name, c.description, c.active, c.createdAt
            ORDER BY c.name
            """)
    List<CategoryAdminRow> findAdminRows();

    interface CategoryAdminRow {
        Long getId();

        String getName();

        String getDescription();

        boolean getActive();

        Instant getCreatedAt();

        long getProductCount();
    }
}
