package com.example.reservation_solution.global.lock;

import com.example.reservation_solution.global.exception.LockAcquisitionException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
@Profile("!redis")
public class InMemoryLockExecutor implements LockExecutor {

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private static final long WAIT_TIME_SECONDS = 5;

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) throws InterruptedException {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock(true));
        boolean acquired = lock.tryLock(WAIT_TIME_SECONDS, TimeUnit.SECONDS);
        if (!acquired) {
            throw LockAcquisitionException.timeout();
        }
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void executeWithLock(String lockKey, Runnable runnable) throws InterruptedException {
        executeWithLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }
}
