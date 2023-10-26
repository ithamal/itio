package io.github.ithaml.itio.executor;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-10-23 10:25
 */
public class DefaultEventLoops {

    public static volatile EventLoopGroup bossEventGroup;

    public static volatile EventLoopGroup workEventGroup;

    private static Object mutex = new Object();

    public static EventLoopGroup getBossEventGroup() {
        if (bossEventGroup != null) {
            return bossEventGroup;
        }
        synchronized (mutex) {
            if (bossEventGroup == null) {
                bossEventGroup = new NioEventLoopGroup(1);
            }
        }
        return bossEventGroup;
    }

    public static EventLoopGroup getWorkEventGroup() {
        if (workEventGroup != null) {
            return workEventGroup;
        }
        synchronized (mutex) {
            if (workEventGroup == null) {
                int threads = Runtime.getRuntime().availableProcessors();
                workEventGroup = new NioEventLoopGroup(threads);
            }
        }
        return workEventGroup;
    }

    public static void shutdownAll() {
        if (bossEventGroup != null) {
            bossEventGroup.shutdownGracefully(2, 60, TimeUnit.SECONDS);
        }
        if (workEventGroup != null) {
            workEventGroup.shutdownGracefully(2, 60, TimeUnit.SECONDS);
        }
    }
}
