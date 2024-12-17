package com.doosan.notification.util;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ApiAuto{

    public static void main(String[] args) throws InterruptedException {
        String apiUrl = "http://localhost:8081/products/{productId}/notifications/re-stock";

        // RestTemplate 객체 생성
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        int totalRequests = 10000;
        int startId = 1;

        // 병렬 처리를 위한 스레드 풀 생성 (500개의 요청을 동시에 보낼 수 있도록 조정)
        ExecutorService executor = Executors.newFixedThreadPool(50); // 50 스레드 사용

        // 요청 시작 시간 기록
        long startTime = System.currentTimeMillis();

        for (int productId = startId; productId < startId + totalRequests; productId++) {
            int finalProductId = productId;
            executor.submit(() -> {
                try {
                    // API 호출 URL에 productId 대입
                    String url = apiUrl.replace("{productId}", String.valueOf(finalProductId));

                    // 요청 본문
                    String requestBody = "{}";
                    HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                    // POST 요청 보내기
                    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

                    // 성공 로그 출력
                    System.out.println("요청 성공: Product ID = " + finalProductId + ", 응답 = " + response.getStatusCode());

                } catch (Exception e) {
                    // 예외 발생 시 오류 로그 출력
                    System.err.println("요청 실패: Product ID = " + finalProductId);
                    e.printStackTrace();
                }
            });
        }

        // 모든 작업이 완료될 때까지 기다림
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 요청 종료 시간 기록
        long endTime = System.currentTimeMillis();

        // 총 소요 시간 계산 및 출력
        long totalTime = endTime - startTime;
        System.out.println("총 요청 시간: " + totalTime + "ms");
        System.out.println("평균 요청 시간: " + (totalTime / (double) totalRequests) + "ms");
    }
}

