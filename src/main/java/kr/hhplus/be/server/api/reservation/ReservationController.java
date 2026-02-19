package kr.hhplus.be.server.api.reservation;

import kr.hhplus.be.server.api.reservation.dto.ReservationRequest;
import kr.hhplus.be.server.api.reservation.dto.ReservationResponse;
import kr.hhplus.be.server.application.reservation.ReservationCommand;
import kr.hhplus.be.server.application.reservation.ReservationResult;
import kr.hhplus.be.server.application.reservation.ReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationUseCase reservationUseCase;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationCommand command = new ReservationCommand(
                request.userId(),
                request.concertName(),
                request.concertDate(),
                request.seatCount(),
                request.totalAmount()
        );

        ReservationResult result = reservationUseCase.execute(command);

        ReservationResponse response = new ReservationResponse(
                result.getReservationId(),
                result.getTotalAmount()
        );

        return ResponseEntity.ok(response);
    }
}
