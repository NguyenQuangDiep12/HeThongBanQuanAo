package com.project_shopping.shopee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "product_id",
                        "size",
                        "color"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    @Column(
            nullable = false,
            length = 40
    )
    private String size;

    @Column(
            nullable = false,
            length = 60
    )
    private String color;

    @Column(
            name = "stock_quantity",
            nullable = false
    )
    private int stockQuantity;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;

    public ProductVariant(
            Product product,
            String size,
            String color,
            int stockQuantity,
            BigDecimal price) {

        this.product = product;
        this.size = size;
        this.color = color;
        this.stockQuantity = stockQuantity;
        this.price = price;
    }
}