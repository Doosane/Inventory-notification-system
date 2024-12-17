package com.doosan.notification.repository;

import com.doosan.notification.entity.ProductUserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductUserNotificationRepository extends JpaRepository<ProductUserNotification, Long> {

    @Query("SELECT pun FROM ProductUserNotification pun WHERE pun.product.id = :productId AND pun.isActive = true")
    List<ProductUserNotification> findByProductIdAndIsActiveTrue(@Param("productId") Long productId);


    List<ProductUserNotification> findByProductIdAndIsActiveTrueAndUserIdGreaterThan(Long productId, Long userId);

    //NotificationServiceTest 코드에서 정렬된 데이터 검증
    List<ProductUserNotification> findByProductIdAndIsActiveTrueOrderByUserIdAsc(Long id);

}