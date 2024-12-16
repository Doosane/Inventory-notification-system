package com.doosan.notification.util;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public class ApiRequestAutomation {

    public static void main(String[] args) {
        String apiUrl = "http://localhost:8081/products/{productId}/notifications/re-stock";

        // RestTemplate 객체 생성
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json"); // JSON 요청이라 가정

        // 요청 범위: 1부터 1000까지
        for (int productId = 1; productId <= 1000; productId++) {
            try {
                // API 호출 URL에 productId 대입
                String url = apiUrl.replace("{productId}", String.valueOf(productId));

                // 요청 본문 (필요한 경우 JSON 데이터 추가)
                String requestBody = "{}"; // 본문이 필요 없다면 빈 객체
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                // POST 요청 보내기
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

                // 성공 로그 출력
                System.out.println("요청 성공: Product ID = " + productId + ", 응답 = " + response.getStatusCode());

            } catch (Exception e) {
                // 예외 발생 시 오류 로그 출력
                System.err.println("요청 실패: Product ID = " + productId);
                e.printStackTrace();
            }
        }
    }
}

