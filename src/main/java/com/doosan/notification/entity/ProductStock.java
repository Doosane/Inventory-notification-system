package com.doosan.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product_stock")
public class ProductStock {

    @Id
    @Column(name = "product_id")
    private Long productId; // 상품 ID (Product 테이블과 1:1 관계)

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity; // 재고 수량

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 업데이트 시간

    // 생성자
    public ProductStock(Long productId, Integer stockQuantity) {
        this.productId = productId;
        this.stockQuantity = stockQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    // 재고 감소 메서드
    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    // 재고 증가 메서드 (Optional)
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
