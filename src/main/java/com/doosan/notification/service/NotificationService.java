package com.doosan.notification.service;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import com.doosan.notification.entity.*;
import com.doosan.notification.exception.ResourceNotFoundException;
import com.doosan.notification.repository.*;
import com.doosan.notification.util.NotificationRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class NotificationService {
    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository notificationHistoryRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductUserNotificationRepository userNotificationRepository;
    private final ProductUserNotificationHistoryRepository userNotificationHistoryRepository;
    private final NotificationRateLimiter notificationRateLimiter;

    // 병렬 처리를 위한 스레드 풀 (최대 50개의 동시 작업)
    private final ExecutorService executorService = Executors.newFixedThreadPool(50);

    public NotificationService(ProductRepository productRepository,
                               ProductNotificationHistoryRepository notificationHistoryRepository,
                               ProductUserNotificationRepository userNotificationRepository,
                               ProductUserNotificationHistoryRepository userNotificationHistoryRepository,
                               ProductStockRepository productStockRepository,
                               NotificationRateLimiter notificationRateLimiter) {
        this.productRepository = productRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userNotificationHistoryRepository = userNotificationHistoryRepository;
        this.notificationRateLimiter = notificationRateLimiter;
        this.productStockRepository = productStockRepository;
    }

    // 재입고 알림 전송
    @Transactional
    public ProductNotificationHistoryDTO sendRestockNotification(Long productId) {
        log.info("재입고 알림 전송 시작 - productId: {}", productId);

        // 상품 및 재고 유효성 검증
        Product product = validateProduct(productId);
        ProductStock productStock = validateProductStock(productId);

        // 재입고 회차 증가
        increaseRestockRound(product);

        ProductNotificationHistory notificationHistory = initializeNotificationHistory(product);  // 알림 기록 초기화
        List<ProductUserNotification> userNotifications = getUserNotifications(productId);      // 활성화된 알림 사용자 목록 가져오기

        validateActiveUsers(userNotifications, notificationHistory);  // 활성화된 사용자가 없을 경우 처리
        processNotifications(userNotifications, product, productStock, notificationHistory); // 알림 전송 처리 (병렬)

        finalizeNotificationHistory(notificationHistory, "COMPLETED");    // 알림 상태를 완료로 변경

        return convertToDto(notificationHistory);
    }

    // 수동 재입고 알림 전송
    @Transactional
    public void sendRestockNotificationManually(Long productId) {
        log.info("수동 재입고 알림 전송 시작 - productId: {}", productId);

        Product product = validateProduct(productId);  // 상품 검증
        ProductNotificationHistory latestNotificationHistory = getLatestNotificationHistory(product);    // 최근 알림 기록 가져오기
        List<ProductUserNotification> userNotifications = getRemainingUserNotifications(productId, latestNotificationHistory);   // 이전 알림 이후 남은 사용자 가져오기
        sendNotificationsManually(userNotifications, product, latestNotificationHistory);     // 사용자에게 알림 전송

        // 알림 상태를 완료로 변경
        finalizeNotificationHistory(latestNotificationHistory, "COMPLETED");
    }

    // 상품 존재 여부 검증
    Product validateProduct(Long productId) {
        return productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("상품이 존재하지 않습니다."));
    }


    // 재고 정보 검증
    // 재고를 업데이트할 때 비관적 락을 적용하여 동시에 수정하지 않도록
    private ProductStock validateProductStock(Long productId) {
        return productStockRepository.findByProductIdForUpdate(productId) // 재고 감소 시 비관적 락 사용
                .orElseThrow(() -> new ResourceNotFoundException("재고 정보가 존재하지 않습니다."));
    }

    // 재입고 회차 증가
    private void increaseRestockRound(Product product) {
        product.setRestockRound(product.getRestockRound() + 1);
        productRepository.save(product);
    }

    // 알림 기록 초기화
    private ProductNotificationHistory initializeNotificationHistory(Product product) {
        ProductNotificationHistory notificationHistory = new ProductNotificationHistory();
        notificationHistory.setProduct(product);
        notificationHistory.setRestockRound(product.getRestockRound());
        notificationHistory.setNotificationStatus("IN_PROGRESS");
        notificationHistoryRepository.save(notificationHistory);
        return notificationHistory;
    }

    // 활성화된 사용자 목록 가져오기
    private List<ProductUserNotification> getUserNotifications(Long productId) {
        return userNotificationRepository.findByProductIdAndIsActiveTrue(productId);
    }

    // 활성화된 사용자 검증
    private void validateActiveUsers(List<ProductUserNotification> userNotifications, ProductNotificationHistory notificationHistory) {
        if (userNotifications.isEmpty()) {
            finalizeNotificationHistory(notificationHistory, "NO_ACTIVE_USERS");
            throw new IllegalStateException("활성화된 알림 사용자가 없습니다.");
        }
    }

    // 병렬로 알림 전송
    private void processNotifications(List<ProductUserNotification> userNotifications, Product product,
                                      ProductStock productStock, ProductNotificationHistory notificationHistory) {
        userNotifications.forEach(userNotification -> executorService.submit(() -> {
            try {
                // 속도 제한 확인
                if (!notificationRateLimiter.tryAcquire()) {
                    log.warn("알림 속도 제한 초과 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());
                    return;
                }

                // 알림 전송
                sendNotification(userNotification, product);

                // 재고 감소 처리
                decreaseStock(productStock, notificationHistory);

                // 마지막 알림 사용자 업데이트
                notificationHistory.setLastNotifiedUserId(userNotification.getUserId());
                notificationHistoryRepository.save(notificationHistory);
            } catch (Exception e) {
                log.error("알림 전송 중 에러 - userId: {}, productId: {}", userNotification.getUserId(), product.getId(), e);
            }
        }));
    }

    // 알림 전송 처리 (수동)
    private void sendNotificationsManually(List<ProductUserNotification> userNotifications, Product product,
                                           ProductNotificationHistory notificationHistory) {
        for (ProductUserNotification userNotification : userNotifications) {
            sendNotification(userNotification, product);
            notificationHistory.setLastNotifiedUserId(userNotification.getUserId());
            notificationHistoryRepository.save(notificationHistory);
        }
    }

    // 알림 전송
    private void sendNotification(ProductUserNotification userNotification, Product product) {
        log.info("알림 전송 시작 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());
        ProductUserNotificationHistory userNotificationHistory = new ProductUserNotificationHistory();
        userNotificationHistory.setProduct(product);
        userNotificationHistory.setUserNotification(userNotification);
        userNotificationHistory.setUserId(userNotification.getUserId());
        userNotificationHistory.setRestockRound(product.getRestockRound());
        userNotificationHistoryRepository.save(userNotificationHistory);
        log.info("알림 히스토리 저장 완료 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());
    }

    // 재고 감소 처리
    private void decreaseStock(ProductStock productStock, ProductNotificationHistory notificationHistory) {
        synchronized (productStock) {
            if (productStock.getStockQuantity() <= 0) {
                finalizeNotificationHistory(notificationHistory, "CANCELED_BY_SOLD_OUT");
                throw new IllegalStateException("재고 부족으로 알림이 중단되었습니다.");
            }
            productStock.setStockQuantity(productStock.getStockQuantity() - 1);
            productStockRepository.save(productStock);
        }
    }

    // 최근 알림 기록 가져오기
    private ProductNotificationHistory getLatestNotificationHistory(Product product) {
        return notificationHistoryRepository.findTopByProductOrderByRestockRoundDesc(product)
                .orElseThrow(() -> new ResourceNotFoundException("재입고 알림 기록이 존재하지 않습니다."));
    }

    // 남은 사용자 목록 가져오기
    private List<ProductUserNotification> getRemainingUserNotifications(Long productId, ProductNotificationHistory notificationHistory) {
        Long lastNotifiedUserId = notificationHistory.getLastNotifiedUserId();
        if (lastNotifiedUserId == null) {
            return userNotificationRepository.findByProductIdAndIsActiveTrue(productId);
        }
        return userNotificationRepository.findByProductIdAndIsActiveTrueAndUserIdGreaterThan(productId, lastNotifiedUserId);
    }

    // 알림 상태를 업데이트
    private void finalizeNotificationHistory(ProductNotificationHistory notificationHistory, String status) {
        notificationHistory.setNotificationStatus(status);
        notificationHistoryRepository.save(notificationHistory);
        log.info("알림 상태 변경 완료 - status: {}", status);
    }

    // DTO 변환
    private ProductNotificationHistoryDTO convertToDto(ProductNotificationHistory notificationHistory) {
        ProductNotificationHistoryDTO dto = new ProductNotificationHistoryDTO();
        dto.setId(notificationHistory.getId());
        dto.setProductId(notificationHistory.getProduct().getId());
        dto.setRestockRound(notificationHistory.getRestockRound());
        dto.setNotificationStatus(notificationHistory.getNotificationStatus());
        dto.setLastNotifiedUserId(notificationHistory.getLastNotifiedUserId());
        return dto;
    }
}