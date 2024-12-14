package com.doosan.notification.controller;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import com.doosan.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/products")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 특정 상품 ID에 대해 재입고 알림 전송
    @PostMapping("/{productId}/notifications/re-stock")
    public ResponseEntity<ProductNotificationHistoryDTO> sendRestockNotification(@PathVariable Long productId) {
        log.info("Received productId: {}", productId);
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(productId);
        return ResponseEntity.ok(result);
    }
}
