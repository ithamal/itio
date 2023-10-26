package io.github.ithaml.itio.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author: ken.lin
 * @since: 2023-10-23 14:18
 */
public class ConcurrentUtils {


    public static Future executeChain(EventExecutor executor, Collection<Supplier<Future>> functions) {
        Promise promise = new DefaultPromise(executor);
        executeChain(functions.iterator(), promise);
        return promise;
    }


    public static void executeChain(Iterator<Supplier<Future>> iterator, Promise promise) {
        if (!iterator.hasNext()) {
            promise.setSuccess(null);
            return;
        }
        Supplier<Future> fn = iterator.next();
        fn.get().addListener(future -> {
            if (promise.isCancelled()) {
                return;
            }
            if (future.isCancelled()) {
                promise.setFailure(new InterruptedException());
            } else if (future.isSuccess()) {
                executeChain(iterator, promise);
            } else {
                promise.setFailure(future.cause());
            }
        });
    }

    public static void writeAndFlush(ChannelHandlerContext ctx, Collection<?> msgList, ChannelPromise promise) {
        Iterator<Supplier<Future>> iterator = msgList.stream().map(msg -> (Supplier<Future>) () -> ctx.writeAndFlush(msg)).iterator();
        executeChain(iterator, promise);
    }

    public static void writeAndFlush(ChannelHandlerContext ctx, Stream<?> stream, ChannelPromise promise) {
        Iterator<Supplier<Future>> iterator = stream.map(msg -> (Supplier<Future>) () -> ctx.writeAndFlush(msg)).iterator();
        executeChain(iterator, promise);
    }
}
