package com.doosan.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product_user_notification") // 테이블 이름 명시적으로 지정
public class ProductUserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 알림 설정 ID

    @ManyToOne(fetch = FetchType.LAZY) // Product와 다대일 관계 설정, 지연 로딩 사용
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 연관된 상품 정보

    @Column(name = "user_id", nullable = false)
    private Long userId; // 알림을 설정한 유저 ID

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN")
    private boolean isActive; // 알림 활성화 상태 (true: 활성, false: 비활성)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성시간

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 업데이트시간
}
