package com.doosan.notification.service;

import com.doosan.notification.repository.*;
import com.doosan.notification.util.NotificationRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

@Slf4j
@SpringBootTest
public class NotiTestOneSecondFiveHundredRequestLimit {

    private static final int MAX_REQUESTS_PER_SECOND = 500; // 초당 요청 허용 개수

    @Autowired
    private NotificationRateLimiter rateLimiter; // Spring 빈 주입

    @Test
    public void testRateLimiterWithinThreshold() throws InterruptedException {
        // Mock repositories
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductNotificationHistoryRepository notificationHistoryRepository = mock(ProductNotificationHistoryRepository.class);
        ProductStockRepository productStockRepository = mock(ProductStockRepository.class);
        ProductUserNotificationRepository userNotificationRepository = mock(ProductUserNotificationRepository.class);
        ProductUserNotificationHistoryRepository userNotificationHistoryRepository = mock(ProductUserNotificationHistoryRepository.class);

        log.info("초당 최대 요청 허용 수: {}", MAX_REQUESTS_PER_SECOND);

        // 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        int requestCount = 600;

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                boolean acquired = rateLimiter.tryAcquire();
                if (acquired) {
                    log.info("요청 허용됨");
                } else {
                    log.warn("요청 거부됨");
                }
            });
        }

        // 스레드 종료 및 완료 대기
        executorService.shutdown();
        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
            log.error("테스트 실행 시간 초과");
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("테스트 완료, 소요 시간: {}ms", duration);
    }
}
