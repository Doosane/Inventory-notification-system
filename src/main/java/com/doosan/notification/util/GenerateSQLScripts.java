package com.doosan.notification.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

@SpringBootApplication
public class GenerateSQLScripts implements CommandLineRunner {
    @Value("${jdbcURL}")
    private String jdbcURL;

    @Value("${dbUser}")
    private String dbUser;

    @Value("${dbPassword}")
    private String dbPassword;

    public static void main(String[] args) {
        SpringApplication.run(GenerateSQLScripts.class, args);
    }

    @Override
    public void run(String... args) {
        // SQL 문
        String insertProductSQL = "INSERT INTO Product (id, restock_round, stock_status) VALUES (?, 0, 'IN_STOCK')";
        String insertNotificationSQL = "INSERT INTO product_user_notification (product_id, user_id, is_active, created_at, updated_at) VALUES (?, ?, true, NOW(), NOW())";
        String insertProductStockSQL = "INSERT INTO product_stock (product_id, stock_quantity) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword)) {
            conn.setAutoCommit(false); // 트랜잭션 시작

            try (PreparedStatement productStmt = conn.prepareStatement(insertProductSQL);
                 PreparedStatement notificationStmt = conn.prepareStatement(insertNotificationSQL);
                 PreparedStatement stockStmt = conn.prepareStatement(insertProductStockSQL)) {

                for (int i = 1; i <= 10000; i++) {
                    try {
                        // Product 테이블 데이터 삽입
                        productStmt.setInt(1, i); // id
                        productStmt.executeUpdate();

                        // ProductStock 테이블 데이터 삽입 (재고 추가)
                        stockStmt.setInt(1, i); // product_id
                        stockStmt.setInt(2, 200); // 기본 재고 수량
                        stockStmt.executeUpdate();

                        // product_user_notification 테이블 데이터 삽입
                        notificationStmt.setInt(1, i); // product_id
                        notificationStmt.setInt(2, i - 1); // user_id
                        notificationStmt.executeUpdate();

                    } catch (SQLIntegrityConstraintViolationException e) {

                        // 중복 키 오류 발생 시 해당 반복만 건너뛰기
                        System.out.println("중복 데이터 건너뜀 - ID: " + i);
                        continue;
                    }
                }

                conn.commit(); // 트랜잭션 커밋
                System.out.println("데이터 삽입 성공!");

            } catch (Exception e) {
                conn.rollback(); // 오류 발생 시 롤백
                e.printStackTrace();
                System.out.println("데이터 삽입 실패, 롤백 실행!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
