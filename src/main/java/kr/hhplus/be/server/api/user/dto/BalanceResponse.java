package kr.hhplus.be.server.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BalanceResponse {
    private Long userId;
    private Long balance;
}
