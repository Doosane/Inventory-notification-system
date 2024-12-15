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
@Table(name = "product_notification_history") // 테이블 이름 명시적으로 지정
public class ProductNotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 알림 기록 ID

    @Column(name = "last_notified_user_id")
    private Long lastNotifiedUserId; // 마지막으로 알림을 받은 유저 ID

    @ManyToOne(fetch = FetchType.LAZY) // Product와 다대일 관계 설정, 지연 로딩 사용
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;  // 연관된 상품 정보

    @Column(name = "restock_round", nullable = false)
    private int restockRound; // 재입고 회차

    @Column(name = "notification_status", nullable = false)
    private String notificationStatus; // 알림 발송 상태

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시점
}


