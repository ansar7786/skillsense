package com.skillsense.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginLockoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 5 * 60; // 5 minutes

    private static class Attempt {
        int count;
        Instant lastAttempt;
        Instant lockedUntil;

        Attempt(Instant now) {
            this.count = 1;
            this.lastAttempt = now;
            this.lockedUntil = null;
        }
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public synchronized boolean isLocked(String collegeId) {
        if (collegeId == null) return false;
        String key = collegeId.trim().toLowerCase();
        Attempt attempt = attempts.get(key);
        if (attempt == null) return false;

        Instant now = Instant.now();
        if (attempt.lockedUntil != null) {
            if (now.isBefore(attempt.lockedUntil)) {
                return true;
            } else {
                // Lock has expired, reset
                attempts.remove(key);
                return false;
            }
        }
        return false;
    }

    public synchronized void recordFailedAttempt(String collegeId) {
        if (collegeId == null) return;
        String key = collegeId.trim().toLowerCase();
        Instant now = Instant.now();

        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            attempts.put(key, new Attempt(now));
            return;
        }

        // If previous attempts were more than 5 minutes ago and not locked, reset count
        if (attempt.lockedUntil == null && now.isAfter(attempt.lastAttempt.plusSeconds(LOCK_DURATION_SECONDS))) {
            attempt.count = 1;
            attempt.lastAttempt = now;
            return;
        }

        attempt.count++;
        attempt.lastAttempt = now;

        if (attempt.count >= MAX_ATTEMPTS) {
            attempt.lockedUntil = now.plusSeconds(LOCK_DURATION_SECONDS);
        }
    }

    public synchronized void recordSuccess(String collegeId) {
        if (collegeId == null) return;
        attempts.remove(collegeId.trim().toLowerCase());
    }

    public synchronized void clear() {
        attempts.clear();
    }
}
