package com.doosan.notification.repository;

import com.doosan.notification.entity.ProductStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    // 데드락 방지 추가 조치
    // 재고 감소 시 비관적 락 사용: 재고를 업데이트할 때 비관적 락을 적용하여 동시에 수정하지 않도록 수정
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductStock ps WHERE ps.productId = :productId")
    Optional<ProductStock> findByProductIdForUpdate(@Param("productId") Long productId);


    Optional<ProductStock> findByProductId(Long productId);

}

