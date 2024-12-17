package com.doosan.notification.service;

import com.doosan.notification.entity.Product;
import com.doosan.notification.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j; // Lombok의 로깅 사용
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j // Lombok의 로깅 어노테이션
public class TestValidateProduct {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private NotificationService notificationService;

    public TestValidateProduct() {
        MockitoAnnotations.openMocks(this); // Mock 초기화
        log.info("Mockito Mocks가 초기화되었습니다.");
    }

    @Test
    public void testValidateProduct() {
        // Mock Product Repository
        Long productId = 1L;
        Product mockProduct = new Product(); // mockProduct는 Product 객체의 Mock 인스턴스
        log.info("Given 단계: productId: {}, mockProduct 객체 생성됨", productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        log.info("productRepository.findById(productId) 호출 시 Optional.of(mockProduct) 반환하도록 설정됨");

        // NotificationService의 validateProduct 호출
        log.info("When 단계: validateProduct(productId) 호출 시작");
        Product product = notificationService.validateProduct(productId);
        log.info("validateProduct 호출 완료. 반환된 Product 객체: {}", product);

        // 검증
        log.info("Then 단계: 반환된 Product 객체가 null이 아닌지 검증");
        assertNotNull(product, "Product는 null이 아니어야 합니다.");
        log.info("Product 객체가 null이 아님을 확인");

        verify(productRepository).findById(productId); // 메서드 호출 검증
        log.info("productRepository.findById(productId) 메서드 호출이 검증됨");
    }
}
