package com.finpay.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ConcurrencyHelper {

    private ConcurrencyHelper() {
    }

    public record Result<T>(T value, Throwable error) {
        public boolean succeeded() {
            return error == null;
        }
    }

    /**
     * Runs every task on its own thread and releases them all at the same instant, to maximize
     * contention. A task that throws does not abort the others; its exception is returned.
     * Fails (instead of hanging) if the tasks do not finish within 60 seconds, which is how a
     * deadlock would show up.
     */
    public static <T> List<Result<T>> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Result<T>>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return new Result<T>(task.call(), null);
                    } catch (Throwable t) {
                        return new Result<T>(null, t);
                    }
                }));
            }
            ready.await();
            go.countDown();

            List<Result<T>> results = new ArrayList<>();
            for (Future<Result<T>> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
