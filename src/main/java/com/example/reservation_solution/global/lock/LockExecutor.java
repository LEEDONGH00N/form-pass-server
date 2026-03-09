package com.example.reservation_solution.global.lock;

import java.util.function.Supplier;

public interface LockExecutor {

    <T> T executeWithLock(String lockKey, Supplier<T> supplier) throws InterruptedException;

    void executeWithLock(String lockKey, Runnable runnable) throws InterruptedException;
}
