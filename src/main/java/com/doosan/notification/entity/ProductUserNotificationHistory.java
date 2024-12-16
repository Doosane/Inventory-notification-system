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
@Table(name = "product_user_notification_history")
public class ProductUserNotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false) // user_id  추가 2024-12-15-오후 5:50
    private Long userId; // 유저 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 상품 아이디

    @Column(name = "restock_round", nullable = false)
    private int restockRound; //재입고 회차

    @Column(name = "notified_at", nullable = false)
    private LocalDateTime notifiedAt = LocalDateTime.now(); // 발송 날짜


    public void setUserNotification(ProductUserNotification userNotification) {

    }
}

