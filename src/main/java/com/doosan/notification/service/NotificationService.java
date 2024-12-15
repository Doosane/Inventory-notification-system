package com.doosan.notification.service;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import com.doosan.notification.entity.*;
import com.doosan.notification.exception.ResourceNotFoundException;
import com.doosan.notification.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NotificationService {
    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository notificationHistoryRepository;
    private final ProductUserNotificationRepository userNotificationRepository;
    private final ProductUserNotificationHistoryRepository userNotificationHistoryRepository;

    public NotificationService(ProductRepository productRepository,
                               ProductNotificationHistoryRepository notificationHistoryRepository,
                               ProductUserNotificationRepository userNotificationRepository,
                               ProductUserNotificationHistoryRepository userNotificationHistoryRepository) {
        this.productRepository = productRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userNotificationHistoryRepository = userNotificationHistoryRepository;
    }

    // 재입고 알림 전송
    // 재입고 알림 전송
    @Transactional
    public ProductNotificationHistoryDTO sendRestockNotification(Long productId) {
        log.info("재입고 알림 전송 시작 - productId: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("상품 존재하지 않음 - productId: {}", productId);
                    return new ResourceNotFoundException("상품이 존재하지 않습니다.");
                });

        log.info("상품 확인 완료 - productId: {}, 상품명: {}, 재입고 회차: {}",
                product.getId(), product.getProductName(), product.getRestockRound());

        product.setRestockRound(product.getRestockRound() + 1);
        productRepository.save(product);
        log.info("재입고 회차 증가 - productId: {}, 새로운 회차: {}", productId, product.getRestockRound());

        ProductNotificationHistory notificationHistory = new ProductNotificationHistory();
        notificationHistory.setProduct(product);
        notificationHistory.setRestockRound(product.getRestockRound());
        notificationHistory.setNotificationStatus("IN_PROGRESS");
        notificationHistoryRepository.save(notificationHistory);
        log.info("재입고 알림 기록 생성 - notificationHistoryId: {}", notificationHistory.getId());

        List<ProductUserNotification> userNotifications = userNotificationRepository.findByProductIdAndIsActiveTrue(productId);
        log.info("활성화된 알림 사용자 수 - productId: {}, 사용자 수: {}", productId, userNotifications.size());

        Long lastNotifiedUserId = null;

        for (ProductUserNotification userNotification : userNotifications) {
            try {
                log.info("사용자 알림 처리 - userId: {}, productId: {}", userNotification.getUserId(), productId);
                if (checkStockAvailability(product)) {
                    log.debug("재고 확인 완료 - productId: {}, 재고 상태: {}", productId, product.getStockStatus());
                    sendNotification(userNotification, product);

                    lastNotifiedUserId = userNotification.getUserId();
                    notificationHistory.setLastNotifiedUserId(lastNotifiedUserId);
                    notificationHistoryRepository.save(notificationHistory);
                    log.info("사용자 알림 성공 - userId: {}, productId: {}", userNotification.getUserId(), productId);
                } else {
                    log.warn("재고 부족으로 알림 중단 - productId: {}", productId);
                    notificationHistory.setNotificationStatus("CANCELED_BY_SOLD_OUT");
                    notificationHistoryRepository.save(notificationHistory);
                    break;
                }
            } catch (Exception e) {
                log.error("알림 전송 중 예외 발생 - userId: {}, productId: {}, 에러: {}",
                        userNotification.getUserId(), productId, e.getMessage(), e);
                notificationHistory.setNotificationStatus("CANCELED_BY_ERROR");
                notificationHistoryRepository.save(notificationHistory);
                throw e;
            }
        }

        if (userNotifications.isEmpty()) {
            log.warn("활성화된 알림 사용자가 없음 - productId: {}", productId);
            throw new IllegalStateException("알림을 받을 활성화된 사용자가 없습니다.");
        }

        if (lastNotifiedUserId == null) {
            notificationHistory.setNotificationStatus("NO_ACTIVE_USERS");
            log.info("활성화된 사용자 없음 - productId: {}", productId);
        } else {
            notificationHistory.setNotificationStatus("COMPLETED");
            log.info("재입고 알림 전송 완료 - productId: {}, 마지막 사용자: {}", productId, lastNotifiedUserId);
        }
        notificationHistoryRepository.save(notificationHistory);

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

        ProductUserNotificationHistory userNotificationHistory = new ProductUserNotificationHistory();
        userNotificationHistory.setProduct(product);
        userNotificationHistory.setUserNotification(userNotification);
        userNotificationHistory.setUserId(userNotification.getUserId());
        userNotificationHistory.setRestockRound(product.getRestockRound());
        userNotificationHistoryRepository.save(userNotificationHistory);

        log.info("알림 히스토리 저장 완료 - userId: {}, productId: {}", userNotification.getUserId(), product.getId());
    }

    // 재고 확인 (모의 로직)
    private boolean checkStockAvailability(Product product) {
        log.debug("재고 확인 중 - productId: {}, 재고 상태: {}", product.getId(), product.getStockStatus());
        return true; // 예제에서는 항상 재고가 있다고 가정
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
