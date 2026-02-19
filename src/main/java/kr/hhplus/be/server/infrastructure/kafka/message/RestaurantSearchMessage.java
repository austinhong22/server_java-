package kr.hhplus.be.server.infrastructure.kafka.message;

import java.time.LocalDateTime;

public record RestaurantSearchMessage(
        Long userId,
        String keyword,
        Integer resultCount,
        LocalDateTime searchedAt
) {
}
