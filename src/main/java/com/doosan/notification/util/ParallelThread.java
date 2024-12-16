package com.doosan.notification.util;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelThread {

    private static final int THREAD_POOL_SIZE = 50; // 스레드 풀 크기 (동시 실행 가능 스레드 수)
    private static final NotificationRateLimiter rateLimiter = new NotificationRateLimiter(); // 속도 제한 관리 클래스 인스턴스

    public static void main(String[] args) throws InterruptedException {
        rateLimiter.setMaxRequestsPerSecond(500); // 초당 최대 500개의 요청 허용

        String apiUrl = "http://localhost:8081/products/{productId}/notifications/re-stock"; // API 호출 URL

        RestTemplate restTemplate = new RestTemplate(); // REST API 호출을 위한 RestTemplate 객체

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json"); // 요청 헤더에 Content-Type 설정

        int totalRequests = 600; // 총 요청 수
        int startId = 2000; // 시작 Product ID

        // 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        long startTime = System.currentTimeMillis(); // 요청 시작 시간 기록

        // 1000개의 요청을 병렬로 실행
        for (int productId = startId; productId < startId + totalRequests; productId++) {
            int finalProductId = productId; // Lambda 표현식에서 사용하기 위해 final 변수로 설정

            executor.submit(() -> {
                try {
                    // 요청이 제한에 걸릴 경우 대기
                    while (!rateLimiter.tryAcquire()) {
                        System.out.printf("요청 제한 대기 중: Product ID = %d\n", finalProductId);
                        Thread.sleep(1); // 제한이 풀릴 때까지 1ms 대기
                    }

                    // API 호출
                    String url = apiUrl.replace("{productId}", String.valueOf(finalProductId)); // URL에 Product ID 대입
                    String requestBody = "{}"; // 요청 본문
                    HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers); // 요청 엔티티 생성

                    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class); // POST 요청 전송

                    // 요청 성공 로그 출력
                    System.out.println("요청 성공: Product ID = " + finalProductId + ", 응답 = " + response.getStatusCode());

                } catch (Exception e) {
                    // 요청 실패 시 에러 로그 출력
                    System.err.println("요청 실패: Product ID = " + finalProductId);
                    e.printStackTrace();
                }
            });
        }

        // 스레드 풀이 종료될 때까지 대기
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS); // 최대 10초 대기

        long endTime = System.currentTimeMillis(); // 요청 종료 시간 기록
        long totalTime = endTime - startTime; // 총 요청 시간 계산
        System.out.println("총 요청 시간: " + totalTime + "ms");
        System.out.println("평균 요청 시간: " + (totalTime / (double) totalRequests) + "ms");
    }


    // 요청 제한을 관리하는 내부 클래스
    static class NotificationRateLimiter {

        private int MAX_REQUESTS_PER_SECOND = 500; // 초당 최대 허용 요청 수
        private final AtomicInteger requestCount = new AtomicInteger(0); // 현재 요청 수를 스레드 안전하게 관리
        private long lastResetTime = System.nanoTime(); // 마지막 초기화 시간을 나노초 단위로 기록
        private final ReentrantLock lock = new ReentrantLock(); // 동기화를 위한 ReentrantLock 객체


        // 요청 허용 최대치를 설정
        public void setMaxRequestsPerSecond(int maxRequests) {
            this.MAX_REQUESTS_PER_SECOND = maxRequests;
        }

        // 요청을 허용할 수 있는지 확인
        public boolean tryAcquire() {
            long currentTime = System.nanoTime(); // 현재 시간을 나노초 단위로 가져옴
            lock.lock(); // Lock을 획득하여 동기화

            try {
                // 1초가 경과했는지 확인
                if ((currentTime - lastResetTime) > 1_000_000_000L) {
                    // 1초가 경과하면 요청 카운트 초기화
                    requestCount.set(0);
                    lastResetTime = currentTime;
                    System.out.println("1초가 지났고 다시 500개의 요청만 최대로 허용한다.");
                }

                // 현재 요청 수가 초당 최대 요청 수보다 적은 경우 요청 허용
                if (requestCount.get() < MAX_REQUESTS_PER_SECOND) {
                    int currentRequest = requestCount.incrementAndGet(); // 요청 카운트 증가
                    System.out.printf("요청 허용: 현재 요청 수 = %d/%d\n", currentRequest, MAX_REQUESTS_PER_SECOND);
                    return true; // 요청 허용

                } else {
                    // 요청 수가 초과한 경우 요청 거부
                    System.out.printf("요청 거부: 현재 요청 수 = %d/%d\n", requestCount.get(), MAX_REQUESTS_PER_SECOND);
                    return false; // 요청 거부
                }
            } finally {
                lock.unlock(); // Lock 해제
            }
        }
    }
}
