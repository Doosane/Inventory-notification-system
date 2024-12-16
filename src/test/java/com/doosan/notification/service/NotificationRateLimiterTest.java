package com.doosan.notification.service;

import com.doosan.notification.util.NotificationRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class NotificationRateLimiterTest {

    @Autowired
    private NotificationRateLimiter rateLimiter;

    @BeforeEach
    public void resetLimiter() {
        // 테스트 실행 전 상태 초기화를 보장
        rateLimiter.init();
    }

    @Test
    public void shouldLimitRequestsToConfiguredMaxPerSecond() throws InterruptedException {
        // 1초 동안 최대 허용 요청 수 테스트
        int maxRequests = 500; // 설정 값에 맞게 검증
        for (int i = 0; i < maxRequests; i++) {
            assertTrue(rateLimiter.tryAcquire(), "요청이 허용되어야 한다. 요청: " + (i + 1));
        }

        // 최대 요청 초과
        assertFalse(rateLimiter.tryAcquire(), "요청 한도를 초과했으므로 요청이 거부되어야 한다.");

        // 1초 대기 후 요청 허용 확인
        Thread.sleep(1000);
        assertTrue(rateLimiter.tryAcquire(), "1초 후 요청이 허용되어야 한다.");
    }

    @Test
    public void shouldResetCounterAfterOneSecond() throws InterruptedException {
        int maxRequests = 500;

        // 최대 요청 한도까지 요청
        for (int i = 0; i < maxRequests; i++) {
            assertTrue(rateLimiter.tryAcquire(), "요청이 허용되어야 한다. 요청: " + (i + 1));
        }

        // 1초 대기 후 초기화 및 요청 성공 확인
        Thread.sleep(1000);
        assertTrue(rateLimiter.tryAcquire(), "1초 후 카운터가 초기화되어 요청이 허용되어야 한다.");
    }

    @Test
    public void shouldRejectExcessiveRequestsImmediately() {
        int maxRequests = 500;

        // 최대 요청 초과 후 즉시 실패 확인
        for (int i = 0; i < maxRequests; i++) {
            assertTrue(rateLimiter.tryAcquire(), "요청이 허용되어야 한다. 요청: " + (i + 1));
        }
        assertFalse(rateLimiter.tryAcquire(), "요청 한도를 초과했으므로 즉시 거부되어야 한다.");
    }
}