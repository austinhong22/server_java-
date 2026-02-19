package kr.hhplus.be.server.application.reservation;

import kr.hhplus.be.server.domain.reservation.Reservation;
import kr.hhplus.be.server.domain.reservation.ReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.infrastructure.event.ReservationConfirmedEvent;
import kr.hhplus.be.server.infrastructure.payment.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReservationResult execute(ReservationCommand command) {
        // 1. 사용자 조회
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 예약 생성
        Reservation reservation = Reservation.builder()
                .user(user)
                .concertName(command.concertName())
                .concertDate(command.concertDate())
                .seatCount(command.seatCount())
                .totalAmount(command.totalAmount())
                .build();

        reservationRepository.save(reservation);

        try {
            // 3. 결제 게이트웨이를 통한 결제 처리
            paymentGateway.processPayment(user.getId(), command.totalAmount());

            // 4. 사용자 잔액 차감
            int updatedRows = userRepository.deductBalanceIfAvailable(command.userId(), command.totalAmount());
            if (updatedRows == 0) {
                throw new IllegalArgumentException("잔액이 부족합니다.");
            }

            // 5. 예약 확정 처리
            reservation.confirm();
            reservationRepository.save(reservation);

            // 6. 예약 완료 이벤트 발행 (트랜잭션과 분리)
            ReservationConfirmedEvent event = ReservationConfirmedEvent.builder()
                    .source(this)
                    .reservationId(reservation.getId())
                    .userId(reservation.getUser().getId())
                    .concertName(reservation.getConcertName())
                    .concertDate(reservation.getConcertDate())
                    .seatCount(reservation.getSeatCount())
                    .totalAmount(reservation.getTotalAmount())
                    .build();
            eventPublisher.publishEvent(event);

            return ReservationResult.success(reservation.getId(), command.totalAmount());
        } catch (Exception e) {
            // 결제 실패 시 롤백
            reservation.cancel();
            reservationRepository.save(reservation);
            throw new RuntimeException("예약 처리 중 오류가 발생했습니다.", e);
        }
    }
}
