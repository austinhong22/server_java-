package kr.hhplus.be.server.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalQuantity " +
           "FROM Order o " +
           "JOIN o.orderItems oi " +
           "WHERE o.status = 'COMPLETED' " +
           "AND o.createdAt >= :startDate " +
           "GROUP BY oi.product.id " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDateTime startDate);
}
