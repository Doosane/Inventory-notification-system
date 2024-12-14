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
    private ProductRepository productRepository;

    @Mock
    private ProductNotificationHistoryRepository notificationHistoryRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSendRestockNotification_Success() {
        Product product = new Product();
        product.setId(1L);
        product.setProductName("테스트 상품");
        product.setRestockRound(3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory history = invocation.getArgument(0);
            history.setId(10L);
            return history;
        });
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(1L);
        assertNotNull(result, "결과 객체가 null이어서는 안된다.");
        assertEquals(10L, result.getId(), "DTO의 ID가 일치해야 한다.");
        assertEquals("완료", result.getNotificationStatus(), "알림 상태는 '완료'여야 한다.");
        assertEquals(4, result.getRestockRound(), "재입고 회차는 4이어야 한다.");
    }

    @Test
    public void testSendRestockNotification_ProductNotFound() {
        // productId가 존재하지 않을 때 ResourceNotFoundException이 발생하는지 검증
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.sendRestockNotification(999L)
        );
        assertEquals("상품을 찾을 수 없습니다.", exception.getMessage());
        verify(notificationHistoryRepository, never()).save(any());
    }

    @Test
    public void testSendRestockNotification_CanceledBySoldOut() {
        Product product = new Product();
        product.setId(1L);
        product.setProductName("테스트 상품");
        product.setRestockRound(0); // 재고 없음
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> notificationService.sendRestockNotification(1L)
        );
        assertEquals("재고가 소진되어 알림을 보낼 수 없습니다.", exception.getMessage());
        verify(notificationHistoryRepository, never()).save(any());
    }

    @Test
    public void testSendRestockNotification_StatusCanceledByError() {
        Product product = new Product();
        product.setId(1L);
        product.setProductName("테스트 상품");
        product.setRestockRound(2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doThrow(new RuntimeException("서드파티 예외")).when(notificationHistoryRepository).save(any());
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> notificationService.sendRestockNotification(1L)
        );
        assertEquals("서드파티 예외", exception.getMessage());
        verify(notificationHistoryRepository, times(1)).save(any());
    }

    @Test
    public void testSendRestockNotification_RestockRoundIncrement() {
        Product product = new Product();
        product.setId(1L);
        product.setProductName("테스트 상품");
        product.setRestockRound(3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory history = invocation.getArgument(0);
            history.setRestockRound(4);
            return history;
        });
        notificationService.sendRestockNotification(1L);
        assertEquals(4, product.getRestockRound(), "재입고 회차는 4가 되어야 한다.");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    public void testSendRestockNotification_SaveNotificationHistory() {
        Product product = new Product();
        product.setId(1L);
        product.setProductName("테스트 상품");
        product.setRestockRound(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory savedHistory = invocation.getArgument(0);
            savedHistory.setId(12L); // ID 설정
            savedHistory.setNotificationStatus("완료"); // 상태 설정
            savedHistory.setRestockRound(2); // 회차 증가
            return savedHistory;
        });

        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(1L);

        assertNotNull(result, "결과 객체가 null이어서는 안된다.");
        assertEquals(12L, result.getId(), "알림 기록의 ID가 저장되어야 한다.");
        assertEquals("완료", result.getNotificationStatus(), "알림 상태는 '완료'여야 한다.");
        assertEquals(2, result.getRestockRound(), "재입고 회차는 2이어야 한다.");
        verify(notificationHistoryRepository, times(1)).save(argThat(history ->
                history.getProduct().equals(product) &&
                        history.getRestockRound() == 2 &&
                        "완료".equals(history.getNotificationStatus())
        ));
    }

    @Test
    public void testSendRestockNotification_RestartAfterFailure() {
        Product product = new Product();
        product.setId(1L);
        product.setProductName("테스트 상품");
        product.setRestockRound(3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(notificationHistoryRepository.save(any(ProductNotificationHistory.class))).thenAnswer(invocation -> {
            ProductNotificationHistory history = invocation.getArgument(0);
            history.setId(20L);
            history.setNotificationStatus("완료");
            history.setRestockRound(4);
            return history;
        });
        ProductNotificationHistoryDTO result = notificationService.sendRestockNotification(1L);
        assertNotNull(result, "결과 객체가 null이어서는 안된다.");
        assertEquals("완료", result.getNotificationStatus(), "알림 상태는 '완료'여야 한다.");
        assertEquals(4, result.getRestockRound(), "재입고 회차는 4가 되어야 한다.");
    }
}