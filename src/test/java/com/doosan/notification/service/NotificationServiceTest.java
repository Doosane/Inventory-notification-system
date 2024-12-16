package com.doosan.notification.service;

import com.doosan.notification.dto.ProductNotificationHistoryDTO;
import com.doosan.notification.entity.Product;
import com.doosan.notification.entity.ProductNotificationHistory;
import com.doosan.notification.exception.ResourceNotFoundException;
import com.doosan.notification.repository.ProductNotificationHistoryRepository;
import com.doosan.notification.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotificationServiceTest {

    @Mock
    private ProductRepository productRepository; // ProductRepository를 Mock으로 설정

    @Mock
    private ProductNotificationHistoryRepository notificationHistoryRepository; // ProductNotificationHistoryRepository를 Mock으로 설정

    @InjectMocks
    private NotificationService notificationService; // NotificationService에 Mock 주입

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // 테스트 실행 전 Mock 초기화
    }

    // 성공적으로 재입고 알림을 보냈을 때의 테스트
    @Test
    public void testSendRestockNotification_Success() {
        Product product = new Product();
        product.setId(1L);

        product.setRestockRound(3); // 초기 재입고 회차는 3
        when(productRepository.findById(1L)).thenReturn(Optional.of(product)); // 상품 조회 성공 시 Mock 응답 설정
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory history = invocation.getArgument(0);
            history.setId(10L); // 저장된 알림 기록 ID
            return history;
        });
        // 알림 전송 실행
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(1L);
        // 결과 검증
        assertNotNull(result, "결과 객체가 null이어서는 안된다."); // 결과 객체 null 확인
        assertEquals(10L, result.getId(), "DTO의 ID가 일치해야 한다."); // DTO의 ID 확인
        assertEquals("완료", result.getNotificationStatus(), "알림 상태는 '완료'여야 한다."); // 알림 상태 확인
        assertEquals(4, result.getRestockRound(), "재입고 회차는 4이어야 한다."); // 재입고 회차가 올바르게 증가했는지 확인
    }

    @Test
    public void testSendRestockNotification_ProductNotFound() {
        // productId가 존재하지 않을 때 ResourceNotFoundException이 발생하는지 검증
        when(productRepository.findById(999L)).thenReturn(Optional.empty()); // 상품이 존재하지 않는 경우 Mock 설정
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.sendRestockNotification(999L) // 없는 상품 ID로 호출
        );
        // 예외 메시지 검증
        assertEquals("상품을 찾을 수 없습니다.", exception.getMessage());
        verify(notificationHistoryRepository, never()).save(any()); // 알림 기록 저장 로직이 호출되지 않았는지 검증
    }

    @Test // 재고가 소진된 경우 예외 발생 검증  IllegalStateException
    public void testSendRestockNotification_CanceledBySoldOut() {
        Product product = new Product();
        product.setId(1L);

        product.setRestockRound(0); // 재고 없음
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> notificationService.sendRestockNotification(1L)
        );
        // 예외 메시지 확인
        assertEquals("재고가 소진되어 알림을 보낼 수 없습니다.", exception.getMessage());
        verify(notificationHistoryRepository, never()).save(any()); // 알림 기록 저장 로직이 호출되지 않았는지 확인
    }

    @Test // 알림 기록 저장 중 서드파티 예외 발생 시 처리 검증
    public void testSendRestockNotification_StatusCanceledByError() {
        Product product = new Product();
        product.setId(1L);

        product.setRestockRound(2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product)); // 상품 조회 성공
        doThrow(new RuntimeException("서드파티 예외")).when(notificationHistoryRepository).save(any()); // 알림 기록 저장 시 예외 발생 설정
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> notificationService.sendRestockNotification(1L) // 알림 전송 시도
        );
        // 예외 메시지 검증
        assertEquals("서드파티 예외", exception.getMessage());
        verify(notificationHistoryRepository, times(1)).save(any()); // 알림 기록 저장 로직이 1회 호출되었는지 확인
    }

    @Test
    public void testSendRestockNotification_RestockRoundIncrement() {
        // 재입고 회차가 올바르게 증가하는지 검증
        Product product = new Product();
        product.setId(1L);

        product.setRestockRound(3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product)); // 상품 조회 성공
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory history = invocation.getArgument(0);
            history.setRestockRound(4); // 회차 증가
            return history;
        });
        // 알림 전송 실행
        notificationService.sendRestockNotification(1L);
        // 상품의 재입고 회차가 올바르게 증가했는지 확인
        assertEquals(4, product.getRestockRound(), "재입고 회차는 4가 되어야 한다.");
        verify(productRepository, times(1)).save(product); // 상품 저장 호출 여부 검증
    }

    @Test  // 알림 기록 저장 검증
    public void testSendRestockNotification_SaveNotificationHistory() {
        Product product = new Product();
        product.setId(1L);

        product.setRestockRound(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product)); // 상품 조회 성공
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory savedHistory = invocation.getArgument(0);
            savedHistory.setId(12L); // 저장된 알림 기록 ID
            savedHistory.setNotificationStatus("완료"); // 상태 설정
            savedHistory.setRestockRound(2); // 회차 증가
            return savedHistory;
        });

        // 알림 전송 실행
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(1L);

        // 결과 검증
        assertNotNull(result, "결과 객체가 null이어서는 안된다."); // 결과 객체 null 확인
        assertEquals(12L, result.getId(), "알림 기록의 ID가 저장되어야 한다.");  // ID 검증
        assertEquals("완료", result.getNotificationStatus(), "알림 상태는 '완료'여야 한다.");  // 상태 검증
        assertEquals(2, result.getRestockRound(), "재입고 회차는 2이어야 한다."); // 회차 검증

        // 저장된 알림 기록의 데이터가 올바른지 확인
        verify(notificationHistoryRepository, times(1)).save(argThat(history ->
                history.getProduct().equals(product) &&
                        history.getRestockRound() == 2 &&
                        "완료".equals(history.getNotificationStatus())
        ));
    }

    @Test
    public void testSendRestockNotification_RestartAfterFailure() {
        // 실패 후 재시도 시 알림이 성공적으로 처리되는지 검증

        Product product = new Product();
        product.setId(1L);

        product.setRestockRound(3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product)); // 상품 조회 성공
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory history = invocation.getArgument(0);
            history.setId(20L); // 저장된 알림 기록 ID
            history.setNotificationStatus("완료"); // 상태 설정
            history.setRestockRound(4); // 회차 증가
            return history;
        });
        // 알림 전송 실행
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(1L);
        // 결과 검증
        assertNotNull(result, "결과 객체가 null이면 안된다."); // 결과 객체 null 확인
        assertEquals("완료", result.getNotificationStatus(), "알림 상태는 '완료'여야 한다."); // 상태 검증
        assertEquals(4, result.getRestockRound(), "재입고 회차는 4가 되어야 한다."); // 회차 검증
    }
}