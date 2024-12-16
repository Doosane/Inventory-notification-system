package com.doosan.notification.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class ApiRequest500Test {

    // RestTemplate 객체를 Mocking하여 실제 요청을 시뮬레이션
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

    @Test
    public void testApiRequestsWithRateLimiter() throws InterruptedException {
        String apiUrl = "http://localhost:8081/products/{productId}/notifications/re-stock";

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json"); // JSON 형식 요청이라고 가정

        int totalRequests = 500; // 총 요청 수
        int startId = 1000; // 시작 Product ID
        ExecutorService executor = Executors.newFixedThreadPool(50); // 50개의 스레드로 병렬 처리

        // RestTemplate Mocking: 요청이 성공했다고 가정
        Mockito.when(restTemplate.postForEntity(anyString(), Mockito.any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        // 실행 시작 시간 기록
        long startTime = System.currentTimeMillis();

        // 총 500개의 요청을 병렬로 전송
        for (int productId = startId; productId < startId + totalRequests; productId++) {
            int finalProductId = productId; // Lambda 표현식에서 사용하기 위해 final 변수로 선언
            executor.submit(() -> {
                try {
                    // URL에 Product ID를 삽입하여 완전한 URL 생성
                    String url = apiUrl.replace("{productId}", String.valueOf(finalProductId));
                    String requestBody = "{}"; // 요청 본문
                    HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                    // API 호출 시뮬레이션
                    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

                    // 응답 상태가 200 OK인지 확인
                    assertTrue(response.getStatusCode().is2xxSuccessful(),
                            "요청 실패 - Product ID: " + finalProductId);

                } catch (Exception e) {
                    // 예외 발생 시 오류 출력
                    System.err.println("요청 실패: Product ID = " + finalProductId);
                    e.printStackTrace();
                }
            });
        }

        // 모든 작업이 완료될 때까지 대기
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS); // 최대 10초 대기

        // 실행 종료 시간 기록
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime; // 총 소요 시간 계산

        // 결과 출력
        System.out.println("총 요청 시간: " + totalTime + "ms");
        System.out.println("평균 요청 시간: " + (totalTime / (double) totalRequests) + "ms");

        // 총 요청 시간이 합리적인 범위 내인지 확인
        assertTrue(totalTime < 2000, "요청 완료 시간이 너무 오래 걸렸습니다.");
    }
}
