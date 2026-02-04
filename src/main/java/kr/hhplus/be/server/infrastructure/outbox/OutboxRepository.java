package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    @Query("SELECT o FROM Outbox o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<Outbox> findAllPending();

    @Query("SELECT o FROM Outbox o WHERE o.status = 'PENDING' AND o.eventType = :eventType ORDER BY o.createdAt ASC")
    List<Outbox> findAllPendingByEventType(@Param("eventType") String eventType);
}
