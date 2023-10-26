package io.github.ithaml.itio.concurrent.impl;

import io.github.ithaml.itio.concurrent.RateLimiter;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: ken.lin
 * @since: 2023-10-20 21:22
 */
public class LeakyBucketRateLimiter implements RateLimiter {
    private final EventExecutor executor;

    private final int size;
    private final AtomicInteger counter = new AtomicInteger();
    private final LinkedBlockingQueue<Promise> waitPromises;

    public LeakyBucketRateLimiter(EventExecutor executor, int size) {
        this.executor = executor;
        this.size = size;
        this.waitPromises = new LinkedBlockingQueue<>();
    }

    @Override
    public Future acquire() {
        DefaultPromise promise = new DefaultPromise(executor);
        if (tryAcquire()) {
            promise.setSuccess(true);
        } else {
            waitPromises.add(promise);
        }
        return promise;
    }

    @Override
    public boolean tryAcquire() {
        if (counter.incrementAndGet() > size) {
            counter.decrementAndGet();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void release() {
        Promise promise = waitPromises.poll();
        if (promise != null) {
            promise.setSuccess(null);
        } else {
            counter.decrementAndGet();
        }
    }
}
