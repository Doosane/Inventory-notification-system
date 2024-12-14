package com.doosan.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductNotificationHistoryDTO {

    private Long id; // 알림 기록 ID
    private Long productId; // 상품 ID
    private int restockRound; // 재입고 회차
    private String notificationStatus; // 알림 상태
    private Long lastNotifiedUserId; // 마지막 알림 사용자 ID
}
