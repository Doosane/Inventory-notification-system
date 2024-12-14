package com.doosan.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductNotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 알림 기록 ID

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 상품과의 연관 관계 @ManyToOne

    private int restockRound; // 재입고 회차

    private String notificationStatus; // 알림 상태

    private Long lastNotifiedUserId; // 마지막 알림 사용자 ID
}
