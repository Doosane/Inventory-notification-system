package com.doosan.notification.repository;

import com.doosan.notification.entity.ProductNotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.doosan.notification.entity.Product;

import java.util.Optional;

// 재입고 알림 기록 데이터 관리용 리포지토리
@Repository
public interface ProductNotificationHistoryRepository extends JpaRepository<ProductNotificationHistory, Long> {
    Optional<ProductNotificationHistory> findTopByProductOrderByRestockRoundDesc(Product product);

    ProductNotificationHistory findByProductId(Long id);
}

