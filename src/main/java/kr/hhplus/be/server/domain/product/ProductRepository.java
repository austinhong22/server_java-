package kr.hhplus.be.server.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIds(@Param("ids") List<Long> ids);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    /**
     * 조건부 UPDATE를 사용한 재고 차감 (동시성 제어)
     * 재고가 충분한 경우에만 차감하고, 영향받은 행 수를 반환
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @return 영향받은 행 수 (1이면 성공, 0이면 재고 부족)
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
