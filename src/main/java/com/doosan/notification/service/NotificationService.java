package com.doosan.notification.service;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import com.doosan.notification.entity.*;
import com.doosan.notification.exception.ResourceNotFoundException;
import com.doosan.notification.repository.*;
import com.doosan.notification.util.NotificationRateLimiter;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class NotificationService {
    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository notificationHistoryRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductUserNotificationRepository userNotificationRepository;
    private final ProductUserNotificationHistoryRepository userNotificationHistoryRepository;
    private final NotificationRateLimiter notificationRateLimiter;

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

    // 재입고 알림 전송 오후 5:52 , 재고 수정
    @Transactional
    public ProductNotificationHistoryDTO sendRestockNotification(Long productId) {
        log.info("재입고 알림 전송 시작 - productId: {}", productId);

        // 상품 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("상품이 존재하지 않습니다."));

        // 재고 확인
        ProductStock productStock = productStockRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("재고 정보가 존재하지 않습니다."));

        // 재입고 회차 증가
        product.setRestockRound(product.getRestockRound() + 1);
        productRepository.save(product);

        // 알림 기록 초기화
        ProductNotificationHistory notificationHistory = new ProductNotificationHistory();
        notificationHistory.setProduct(product);
        notificationHistory.setRestockRound(product.getRestockRound());
        notificationHistory.setNotificationStatus("IN_PROGRESS");
        notificationHistoryRepository.save(notificationHistory);

        // 알림 대상 사용자 가져오기
        List<ProductUserNotification> userNotifications = userNotificationRepository.findByProductIdAndIsActiveTrue(productId);

        if (userNotifications.isEmpty()) {
            notificationHistory.setNotificationStatus("NO_ACTIVE_USERS");
            notificationHistoryRepository.save(notificationHistory);
            throw new IllegalStateException("활성화된 알림 사용자가 없습니다.");
        }

        // 랜덤 객체 생성
        Random random = new Random();

        // 알림 전송 및 재고 감소
        try {
            for (ProductUserNotification userNotification : userNotifications) {
                // 알림 전송
                sendNotification(userNotification, product);

                // 랜덤 구매 여부 (50% 확률)
                boolean willBuy = random.nextBoolean();

                if (willBuy) {
                    log.info("사용자가 상품 구매 결정 - userId: {}", userNotification.getUserId());

                    // 재고 감소
                    if (productStock.getStockQuantity() <= 0) {
                        notificationHistory.setNotificationStatus("CANCELED_BY_SOLD_OUT");
                        notificationHistoryRepository.save(notificationHistory);
                        log.warn("재고 부족으로 알림 중단 - productId: {}", productId);
                        return convertToDto(notificationHistory);
                    }
                    productStock.setStockQuantity(productStock.getStockQuantity() - 1);
                    productStockRepository.save(productStock);

                } else {
                    log.info("사용자가 상품 구매하지 않음 - userId: {}", userNotification.getUserId());
                    // 재고 감소
                    if (productStock.getStockQuantity() <= 0) {
                        notificationHistory.setNotificationStatus("CANCELED_BY_SOLD_OUT");
                        notificationHistoryRepository.save(notificationHistory);
                        log.warn("재고 부족으로 알림 중단 - productId: {}", productId);
                        return convertToDto(notificationHistory);
                    }
                }

                // 알림 성공 기록
                notificationHistory.setLastNotifiedUserId(userNotification.getUserId());
                notificationHistoryRepository.save(notificationHistory);
            }

            // 모든 알림 완료
            notificationHistory.setNotificationStatus("COMPLETED");
            notificationHistoryRepository.save(notificationHistory);
        } catch (Exception e) {
            notificationHistory.setNotificationStatus("CANCELED_BY_ERROR");
            notificationHistoryRepository.save(notificationHistory);
            log.error("알림 전송 중 예외 발생 - productId: {}, 에러: {}", productId, e.getMessage(), e);
            throw e;
        }

        return convertToDto(notificationHistory);
    }


    // 수동 재입고 알림 전송
    @Transactional
    public void sendRestockNotificationManually(Long productId) {
        log.info("수동 재입고 알림 전송 시작 - productId: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("상품 존재하지 않음 - productId: {}", productId);
                    return new ResourceNotFoundException("상품이 존재하지 않습니다.");
                });

        ProductNotificationHistory latestNotificationHistory = notificationHistoryRepository.findTopByProductOrderByRestockRoundDesc(product)
                .orElseThrow(() -> {
                    log.error("재입고 알림 기록 없음 - productId: {}", productId);
                    return new ResourceNotFoundException("재입고 알림 기록이 존재하지 않습니다.");
                });

        log.info("최근 알림 기록 확인 - productId: {}, 최근 회차: {}", productId, latestNotificationHistory.getRestockRound());

        Long lastNotifiedUserId = latestNotificationHistory.getLastNotifiedUserId();

        List<ProductUserNotification> userNotifications;
        if (lastNotifiedUserId == null) {
            userNotifications = userNotificationRepository.findByProductIdAndIsActiveTrue(productId);
        } else {
            userNotifications = userNotificationRepository.findByProductIdAndIsActiveTrueAndUserIdGreaterThan(productId, lastNotifiedUserId);
        }
        log.info("수동 알림 대상 사용자 수 - productId: {}, 사용자 수: {}", productId, userNotifications.size());

        for (ProductUserNotification userNotification : userNotifications) {
            sendNotification(userNotification, product);

            latestNotificationHistory.setLastNotifiedUserId(userNotification.getUserId());
            notificationHistoryRepository.save(latestNotificationHistory);
            log.info("사용자 알림 성공 - userId: {}, productId: {}", userNotification.getUserId(), productId);
        }

        latestNotificationHistory.setNotificationStatus("COMPLETED");
        notificationHistoryRepository.save(latestNotificationHistory);
        log.info("수동 재입고 알림 전송 완료 - productId: {}", productId);
    }

    // 알림 전송
    private void sendNotification(ProductUserNotification userNotification, Product product) {
        log.info("알림 전송 시작 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());

//        if (!notificationRateLimiter.tryAcquire()) {
//            log.warn("알림 속도 제한 초과 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());
//            throw new IllegalStateException("알림 속도 제한 초과");
//        }

        ProductUserNotificationHistory userNotificationHistory = new ProductUserNotificationHistory();
        userNotificationHistory.setProduct(product);
        userNotificationHistory.setUserNotification(userNotification);
        userNotificationHistory.setUserId(userNotification.getUserId());
        userNotificationHistory.setRestockRound(product.getRestockRound());
        userNotificationHistoryRepository.save(userNotificationHistory);

        log.info("알림 히스토리 저장 완료 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());
    }

    // 재고 확인
    private boolean checkStockAvailability(Product product) {
        log.debug("재고 확인 중 - productId: {}, 재고 상태: {}", product.getId(), product.getStockStatus());
        return true;
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
