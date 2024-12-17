package com.doosan.notification.repository;

import com.doosan.notification.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 비관적 락을 사용하여 한 번에 하나의 트랜잭션만 특정 행을 업데이트하도록 제한
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 비관적 락(Pessimistic Lock) 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE) // @Lock 어노테이션 추가
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdWithLock(Long productId);




}
