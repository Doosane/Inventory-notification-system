package com.doosan.notification.controller;

import com.doosan.notification.entity.Product;
import com.doosan.notification.repository.ProductRepository;
import com.doosan.notification.repository.ProductNotificationHistoryRepository;
import com.doosan.notification.repository.ProductUserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductNotificationHistoryRepository productNotificationHistoryRepository;

    @Autowired
    private ProductUserNotificationRepository productUserNotificationRepository;

    @BeforeEach
    void setup() {
        // 데이터 정리: 관계 엔티티 먼저 삭제
        productNotificationHistoryRepository.deleteAll();
        productUserNotificationRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void testSendRestockNotification_ProductNotFound() throws Exception {
        mockMvc.perform(post("/products/{productId}/notifications/re-stock", 999L)) // 존재하지 않는 ID
                .andExpect(status().isNotFound())
                .andExpect(content().string("상품을 찾을 수 없습니다."));
    }

    @Test
    void testSendRestockNotification_StoppedBySoldOut() throws Exception {
        // Given: 재고가 없는 상품
        Product product = Product.builder()
                .productName("상품 A")
                .restockRound(0) // 재고 없음
                .build();
        productRepository.save(product);

        // When & Then
        mockMvc.perform(post("/products/{productId}/notifications/re-stock", product.getId()))
                .andExpect(status().isBadRequest()) // 글로벌 핸들러에서 BAD_REQUEST 반환
                .andExpect(content().string("재고가 소진되어 알림을 보낼 수 없습니다."));
    }


}