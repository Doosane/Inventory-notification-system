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

    // 재입고 알림 전송 API
    @PostMapping("/{productId}/notifications/re-stock")
    public ResponseEntity<ProductNotificationHistoryDTO> sendRestockNotification(@PathVariable Long productId) {
        log.info("Processing restock notification for productId: {}", productId);
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(productId);
        return ResponseEntity.ok(result);
    }

    // 수동 재입고 알림 전송 API
    @PostMapping("/admin/{productId}/notifications/re-stock")
    public ResponseEntity<Void> sendRestockNotificationManually(@PathVariable Long productId) {
        log.info("Manually processing restock notification for productId: {}", productId);
        notificationService.sendRestockNotificationManually(productId);
        return ResponseEntity.ok().build();
    }
}
