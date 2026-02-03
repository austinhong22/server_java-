package kr.hhplus.be.server.service.user;

import kr.hhplus.be.server.api.user.dto.BalanceResponse;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .balance(5000L)
                .build();
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePointSuccess() {
        // given
        Long userId = 1L;
        Long chargeAmount = 3000L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        BalanceResponse result = userService.chargePoint(userId, chargeAmount);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBalance()).isEqualTo(8000L); // 5000 + 3000
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("포인트 충전 실패 - 사용자를 찾을 수 없음")
    void chargePointUserNotFound() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.chargePoint(userId, 3000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalanceSuccess() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        BalanceResponse result = userService.getBalance(userId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBalance()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("잔액 조회 실패 - 사용자를 찾을 수 없음")
    void getBalanceUserNotFound() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getBalance(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
}
