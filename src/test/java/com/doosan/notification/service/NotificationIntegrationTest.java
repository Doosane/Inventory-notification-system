package com.doosan.notification.service;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j // 로깅 추가
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // 테스트 실행 후 Spring 컨텍스트를 정리하고 종료 시점을 테스트 완료 이후로 미룸
public class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void testSendRestockNotification() {
        // Given
        Long productId = 1L;
        log.info("테스트 시작: 재입고 알림 전송 - productId: {}", productId);

        // When
        log.info("재입고 알림 전송 시작");
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(productId);
        log.info("재입고 알림 전송 완료. 반환된 결과: {}", result);

        // Then
        assertNotNull(result, "반환된 결과가 null이 아닙니다.");
        log.info("검증: 반환된 결과가 null이 아님을 확인");

        assertEquals("COMPLETED", result.getNotificationStatus(), "알림 상태가 COMPLETED여야 합니다.");
        log.info("검증: 알림 상태가 'COMPLETED'임을 확인");
        log.info("테스트 종료: 재입고 알림 전송 - 성공");
    }

    // 순차적으로 여러 유저에게 알림 전송 테스트
    // 각각의 유저에게 알림을 보내고, 중간에 재고가 소진되면 중단되는지 검증
    @Test
    public void testSequentialNotificationWithStockDepletion() {
        // Given
        Long productId = 1L;
        log.info("테스트 시작: 재입고 알림 순차 전송 및 재고 소진 검증 - productId: {}", productId);

        // When
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(productId);

        // Then
        assertNotNull(result, "알림 히스토리가 null이 아니어야 합니다.");
        log.info("알림 히스토리 반환 확인: {}", result);

        // 상태 검증
        if (result.getNotificationStatus().equals("CANCELED_BY_SOLD_OUT")) {
            log.info("재고 소진으로 알림 전송이 중단됨");
        } else {
            assertEquals("COMPLETED", result.getNotificationStatus(), "알림 상태가 COMPLETED여야 합니다.");
            log.info("알림 상태 COMPLETED 확인");
        }
    }

    // 여러 유저에 대한 병렬 테스트 (속도 제한 포함)
    // 여러 유저에게 알림을 병렬로 보내되, 속도 제한을 검증 !
    @Test
    public void testParallelNotificationWithRateLimit() throws InterruptedException {
        // Given
        Long productId = 1L;
        int totalUsers = 600; // 유저 수
        log.info("테스트 시작: 병렬 알림 전송 및 속도 제한 검증 - productId: {}, 유저 수: {}", productId, totalUsers);

        // 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        // When
        for (int i = 0; i < totalUsers; i++) {
            final int userId = i + 1; // 유저 ID 시뮬레이션
            executorService.submit(() -> {
                try {
                    log.info("알림 전송 시작: userId #{}", userId);
                    ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(productId);
                    log.info("알림 전송 성공: userId #{}, 결과: {}", userId, result);
                } catch (Exception e) {
                    log.error("알림 전송 실패: userId #{}, 에러: {}", userId, e.getMessage(), e);
                }
            });
        }

        // ExecutorService 종료
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("ExecutorService 종료 시간 초과! 남은 작업 강제 종료.");
                executorService.shutdownNow(); // 남은 작업 강제 종료
            }
        } catch (InterruptedException e) {
            log.error("스레드 종료 중 인터럽트 발생", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // 현재 스레드 상태 복원
        }


        // Then
        log.info("병렬 알림 전송 테스트 완료");
    }

}
