package io.github.ithaml.itio.concurrent.impl;

import io.github.ithaml.itio.concurrent.RateLimiter;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: ken.lin
 * @since: 2023-10-22 10:35
 */
public class CombineRateLimiter implements RateLimiter {

    private final EventExecutor executor;
    private final List<RateLimiter> limiters;

    public CombineRateLimiter(EventExecutor executor, List<RateLimiter> limiters) {
        this.executor = executor;
        this.limiters = limiters;
    }

    @Override
    public Future acquire() {
        Promise promise = new DefaultPromise(executor);
        executeChain(new LinkedBlockingQueue<>(limiters), promise);
        return promise;
    }

    private void executeChain(LinkedBlockingQueue<RateLimiter> queue, Promise promise) {
        RateLimiter limiter = queue.poll();
        if (limiter == null) {
            promise.setSuccess(null);
            return;
        }
        limiter.acquire().addListener((ChannelFutureListener) future -> {
            if (promise.isCancelled()) {
                return;
            }
            if (future.isCancelled() && future.isSuccess()) {
                executeChain(queue, promise);
            } else {
                promise.setFailure(future.cause());
            }
        });
    }

    @Override
    public boolean tryAcquire() {
        List<RateLimiter> buffer = new ArrayList<>();
        for (RateLimiter limiter : limiters) {
            if (limiter.tryAcquire()) {
                buffer.add(limiter);
            } else {
                break;
            }
        }
        if (buffer.size() == limiters.size()) {
            return true;
        }
        for (RateLimiter limiter : buffer) {
            limiter.release();
        }
        return false;
    }

    @Override
    public void release() {
        for (RateLimiter limiter : this.limiters) {
            limiter.release();
        }
    }

}
