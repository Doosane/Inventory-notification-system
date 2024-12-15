package com.doosan.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductUserNotificationHistoryDTO {
    private Long id;
    private Long productId;
    private Long userId;
    private int restockRound;
    private LocalDateTime notifiedAt;
}