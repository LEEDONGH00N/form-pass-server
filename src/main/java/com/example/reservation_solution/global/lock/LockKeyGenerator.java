package com.example.reservation_solution.global.lock;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LockKeyGenerator {

    private static final String SCHEDULE_LOCK_PREFIX = "schedule:";

    public static String schedule(Long scheduleId) {
        return SCHEDULE_LOCK_PREFIX + scheduleId;
    }
}
