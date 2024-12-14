package com.doosan.notification.service;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import com.doosan.notification.entity.Product;
import com.doosan.notification.entity.ProductNotificationHistory;
import com.doosan.notification.exception.ResourceNotFoundException;
import com.doosan.notification.repository.ProductNotificationHistoryRepository;
import com.doosan.notification.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private final ProductRepository productRepository; // 상품 정보 관리 리포지토리
    private final ProductNotificationHistoryRepository notificationHistoryRepository; // 알림 기록 관리 리포지토리

    public NotificationService(ProductRepository productRepository,
                               ProductNotificationHistoryRepository notificationHistoryRepository) {
        this.productRepository = productRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
    }



    @Transactional
    public ProductNotificationHistoryDTO sendRestockNotification(Long productId) { // 재입고 알림 전송
        // 상품 ID로 상품 조회. 없으면 예외 발생.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다."));
        log.info("조회된 상품: {}", product);

        if (product.getRestockRound() == 0) {
            throw new IllegalStateException("재고가 소진되어 알림을 보낼 수 없습니다."); // IllegalStateException 예외처리
        }

        // 상품의 재입고 회차 증가
        product.setRestockRound(product.getRestockRound() + 1);
        productRepository.save(product);

        // 재입고 알림 기록 생성 및 저장
        ProductNotificationHistory notificationHistory = new ProductNotificationHistory();
        notificationHistory.setProduct(product);
        notificationHistory.setRestockRound(product.getRestockRound());
        notificationHistory.setNotificationStatus("완료"); // 알림 상태를 '완료'로 설정
        notificationHistoryRepository.save(notificationHistory);

        // 알림 기록 엔티티를 DTO로 변환하여 반환
        return convertToDto(notificationHistory);
    }

    //알림 기록 엔티티를 DTO로 변환
    private ProductNotificationHistoryDTO convertToDto(ProductNotificationHistory notificationHistory) {
        ProductNotificationHistoryDTO dto = new ProductNotificationHistoryDTO();
        dto.setId(notificationHistory.getId()); // 알림 기록 ID 설정
        dto.setProductId(notificationHistory.getProduct().getId()); // 상품 ID 설정
        dto.setRestockRound(notificationHistory.getRestockRound()); // 재입고 회차 설정
        dto.setNotificationStatus(notificationHistory.getNotificationStatus()); // 알림 상태 설정
        dto.setLastNotifiedUserId(notificationHistory.getLastNotifiedUserId()); // 마지막 알림 사용자 ID 설정
        return dto;
    }
}
