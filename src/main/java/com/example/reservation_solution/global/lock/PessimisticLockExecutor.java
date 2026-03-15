package com.example.reservation_solution.global.lock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Profile("pessimistic")
public class PessimisticLockExecutor implements LockExecutor {

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) throws InterruptedException {
        return supplier.get();
    }

    @Override
    public void executeWithLock(String lockKey, Runnable runnable) throws InterruptedException {
        runnable.run();
    }
}
