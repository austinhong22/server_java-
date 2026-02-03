package kr.hhplus.be.server.api.user;

import kr.hhplus.be.server.api.user.dto.BalanceResponse;
import kr.hhplus.be.server.api.user.dto.ChargeRequest;
import kr.hhplus.be.server.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/{userId}/charge")
    public ResponseEntity<BalanceResponse> chargePoint(
            @PathVariable Long userId,
            @RequestBody ChargeRequest request) {
        BalanceResponse response = userService.chargePoint(userId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {
        BalanceResponse response = userService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
}
