package com.dgjs.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 같은 이메일로 연속 실패하면 일정 시간 로그인을 막음. 메모리에만 기록하므로 서버를 다시 켜면 초기화됨
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int PURGE_THRESHOLD = 1000;

    private static final class Attempt {
        private int failures;
        private Instant lastFailure;
        private Instant lockedUntil;
    }

    private final Clock clock;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isLocked(String email) {
        Attempt attempt = attempts.get(email);
        if (attempt == null) {
            return false;
        }
        synchronized (attempt) {
            if (attempt.lockedUntil == null) {
                return false;
            }
            if (clock.instant().isBefore(attempt.lockedUntil)) {
                return true;
            }
            attempts.remove(email, attempt);
            return false;
        }
    }

    public void recordFailure(String email) {
        purgeIfNeeded();
        Attempt attempt = attempts.computeIfAbsent(email, key -> new Attempt());
        synchronized (attempt) {
            Instant now = clock.instant();
            // 마지막 실패로부터 잠금 시간이 지났으면 연속 실패로 보지 않음
            if (attempt.lastFailure != null && attempt.lastFailure.plus(LOCK_DURATION).isBefore(now)) {
                attempt.failures = 0;
            }
            attempt.failures++;
            attempt.lastFailure = now;
            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = now.plus(LOCK_DURATION);
            }
        }
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    private void purgeIfNeeded() {
        if (attempts.size() < PURGE_THRESHOLD) {
            return;
        }
        Instant cutoff = clock.instant().minus(LOCK_DURATION);
        attempts.entrySet().removeIf(e -> e.getValue().lastFailure != null && e.getValue().lastFailure.isBefore(cutoff));
    }
}
