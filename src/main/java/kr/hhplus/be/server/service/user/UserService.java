package kr.hhplus.be.server.service.user;

import kr.hhplus.be.server.api.user.dto.BalanceResponse;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public BalanceResponse chargePoint(Long userId, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        user.chargePoint(amount);
        userRepository.save(user);
        
        return new BalanceResponse(userId, user.getBalance());
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        return new BalanceResponse(userId, user.getBalance());
    }
}
