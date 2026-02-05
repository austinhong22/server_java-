package kr.hhplus.be.server.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    /**
     * 조건부 UPDATE를 사용한 잔액 차감 (동시성 제어)
     * 잔액이 충분한 경우에만 차감하고, 영향받은 행 수를 반환
     * @param userId 사용자 ID
     * @param amount 차감할 금액
     * @return 영향받은 행 수 (1이면 성공, 0이면 잔액 부족)
     */
    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance - :amount WHERE u.id = :userId AND u.balance >= :amount")
    int deductBalanceIfAvailable(@Param("userId") Long userId, @Param("amount") Long amount);
}
