package com.doosan.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 상품 아이디

    @Column(name = "restock_round", nullable = false)
    private int restockRound; // 재입고 회차

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false)
    private StockStatus stockStatus; // 재고 상태

    public enum StockStatus {
        IN_STOCK,
        OUT_OF_STOCK
    }
}

