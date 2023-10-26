package io.github.ithaml.itio.concurrent;

import io.netty.util.concurrent.Future;

/**
 * @author: ken.lin
 * @since: 2023-10-20 20:16
 */
public interface RateLimiter {

    /**
     * 强制进入
     */
    Future acquire();

    /**
     * 尝试进入
     *
     * @return
     */
    boolean tryAcquire();

    /**
     * 释放
     */
    void release();
}
