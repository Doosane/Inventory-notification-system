package com.doosan.notification.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(NotificationRateLimiter.class);

    private static final int MAX_REQUESTS_PER_SECOND = 500; // 초당 요청 허용 개수 (하드 코딩)

    private final AtomicInteger requestCount = new AtomicInteger(0); // 현재 요청 수를 저장하는 AtomicInteger (스레드 안전)
    private long lastResetTime = System.nanoTime(); // 마지막 초기화 시점의 시간 (나노초 단위)
    private final Lock lock = new ReentrantLock(); // 동시성 제어를 위한 Lock 객체




    @PostConstruct
    public void init() {
        // 애플리케이션 시작 시 설정된 값 확인
        logger.info("Rate limiter 초기화됨. 설정된 MAX_REQUESTS_PER_SECOND 값: {}", MAX_REQUESTS_PER_SECOND);
    }

    // 요청을 허용할 수 있는지 확인
    public boolean tryAcquire() {
        long currentTime = System.nanoTime(); // 나노초 단위
        lock.lock(); // Lock을 획득하여 동시성 제어
        try {
            // 1초(1_000_000_000 나노초) 경과 시 요청 카운트를 초기화
            if ((currentTime - lastResetTime) > 1_000_000_000L) {
                logger.debug("1초가 경과하여 요청 카운트를 초기화합니다. 현재 요청 수: {}", requestCount.get());
                requestCount.set(0); // 요청 카운트를 0으로 초기화
                lastResetTime = currentTime; // 마지막 초기화 시간을 갱신
            }
            // 현재 요청 수가 허용된 최대 요청 수 이하인지 확인
            if (requestCount.get() < MAX_REQUESTS_PER_SECOND) {
                int currentRequest = requestCount.incrementAndGet(); // 요청 수를 증가
                logger.info("요청 허용됨. 현재 요청 수: {} / {}", currentRequest, MAX_REQUESTS_PER_SECOND);
                return true; // 요청 허용
            } else {
                logger.warn("요청 거부됨. 요청 수 한도 초과: {}/{}", requestCount.get(), MAX_REQUESTS_PER_SECOND);
                return false; // 요청 거부
            }
        } finally {
            logger.trace("Lock 해제 중...");
            lock.unlock(); // Lock 해제
        }
    }
}
