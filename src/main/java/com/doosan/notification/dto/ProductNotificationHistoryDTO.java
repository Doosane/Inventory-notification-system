package com.doosan.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductNotificationHistoryDTO {

    private Long id;
    private Long productId;
    private int restockRound;
    private String notificationStatus;
    private Long lastNotifiedUserId;
}