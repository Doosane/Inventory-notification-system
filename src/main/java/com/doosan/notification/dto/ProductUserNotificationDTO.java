package com.doosan.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductUserNotificationDTO {
    private Long id;
    private Long productId;
    private Long userId;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}