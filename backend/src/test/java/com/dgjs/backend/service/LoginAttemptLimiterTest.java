package com.dgjs.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    private void fail(String email, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email);
        }
    }

    @Test
    void 실패가_한도보다_적으면_잠기지_않는다() {
        fail("a@dgjs.local", LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThat(limiter.isLocked("a@dgjs.local")).isFalse();
    }

    @Test
    void 연속_실패가_한도에_이르면_잠긴다() {
        fail("a@dgjs.local", LoginAttemptLimiter.MAX_FAILURES);

        assertThat(limiter.isLocked("a@dgjs.local")).isTrue();
        assertThat(limiter.isLocked("other@dgjs.local")).isFalse();
    }

    @Test
    void 잠금_시간이_지나면_풀린다() {
        fail("a@dgjs.local", LoginAttemptLimiter.MAX_FAILURES);

        clock.advance(LoginAttemptLimiter.LOCK_DURATION.minusSeconds(1));
        assertThat(limiter.isLocked("a@dgjs.local")).isTrue();

        clock.advance(Duration.ofSeconds(2));
        assertThat(limiter.isLocked("a@dgjs.local")).isFalse();
    }

    @Test
    void 성공하면_실패_기록이_지워진다() {
        fail("a@dgjs.local", LoginAttemptLimiter.MAX_FAILURES - 1);
        limiter.recordSuccess("a@dgjs.local");
        fail("a@dgjs.local", LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThat(limiter.isLocked("a@dgjs.local")).isFalse();
    }

    @Test
    void 오래전_실패는_연속_실패로_세지_않는다() {
        fail("a@dgjs.local", LoginAttemptLimiter.MAX_FAILURES - 1);
        clock.advance(LoginAttemptLimiter.LOCK_DURATION.plusMinutes(1));

        limiter.recordFailure("a@dgjs.local");

        assertThat(limiter.isLocked("a@dgjs.local")).isFalse();
    }
}
