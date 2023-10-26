package io.github.ithaml.itio.client.impl;

import io.github.ithaml.itio.client.Connection;
import io.github.ithaml.itio.exception.ConnectionClosedException;
import io.github.ithaml.itio.util.ConcurrentUtils;
import io.netty.channel.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author: ken.lin
 * @since: 2023-10-20 09:02
 */
public class ConnectionImpl implements Connection {

    private final Channel channel;
    public final AtomicReference<Instant> checkAliveTime;
    private final Map<String, Object> attributeHashMap = new HashMap<>();
    private final Object mutex = new Object();
    private final LinkedBlockingQueue<DefaultPromise> waitPromises = new LinkedBlockingQueue<>();
    private volatile boolean isClosed = false;

    public ConnectionImpl(Channel channel) {
        this.channel = channel;
        this.checkAliveTime = new AtomicReference(Instant.now());
        // 监听通道关闭
        this.closeFuture().addListener((ChannelFutureListener) future -> {
            synchronized (mutex) {
                // 响应所有承诺
                while (true) {
                    DefaultPromise promise = waitPromises.poll();
                    if (promise == null) {
                        break;
                    }
                    if (!promise.isDone()) {
                        promise.setFailure(new ConnectionClosedException());
                    }
                }
            }
        });
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    /**
     * 关闭
     *
     * @return promise
     */
    @Override
    public ChannelFuture close() {
        isClosed = true;
        synchronized (mutex) {
            return channel.close();
        }
    }

    /**
     * 写入消息
     *
     * @param request 请求
     * @return promise
     */
    @Override
    public ChannelFuture writeAndFlush(Object request) {
        return channel.writeAndFlush(request);
    }

    @Override
    public Connection getRealConnection() {
        return this;
    }

    @Override
    public boolean isAlive() {
        if (isClosed) {
            return false;
        }
        return channel.isActive();
    }

    @Override
    public <R> Future<R> writeForResponse(Object request, Class<R> responseClass) {
        return internalWriteForResponses(Arrays.asList(request), responseClass, -1);
    }

    @Override
    public <R> Future<Collection<R>> writeForResponses(Collection requests, Class<R> responseClass) {
        return this.writeForResponses(requests, responseClass, requests.size());
    }

    @Override
    public <R> Future<Collection<R>> writeForResponses(Collection requests, Class<R> responseClass, int size) {
        return internalWriteForResponses(requests, responseClass, size);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributeHashMap.put(name, value);
    }

    @Override
    public <V> V getAttribute(String name) {
        return (V) attributeHashMap.get(name);
    }

    @Override
    public Future closeFuture() {
        return channel.closeFuture();
    }

    private <T, R> DefaultPromise<R> internalWriteForResponses(Collection requests, Class<T> responseClass, int size) {
        DefaultPromise<R> promise = new DefaultPromise<>(channel.eventLoop());
        try {
            synchronized (mutex) {
                if (!this.isAlive()) {
                    promise.setFailure(new ConnectionClosedException());
                } else {
                    String handlerName = "waitForResponse-" + responseClass.getSimpleName();
                    ResponseHandler<T, R> responseHandler = new ResponseHandler<>(responseClass, size, promise);
                    channel.pipeline().addBefore("noop", handlerName, responseHandler);
                    promise.addListener(future -> {
                        waitPromises.remove(promise);
                        channel.pipeline().remove(responseHandler);
                    });
                    waitPromises.add(promise);
                }
            }
            DefaultPromise<R> writePromise = new DefaultPromise<>(channel.eventLoop());
            writePromise.addListener(future -> {
                if (promise.isCancelled()) {
                    return;
                }
                if (!future.isSuccess()) {
                    promise.setFailure(future.cause());
                }
            });
            // 批量写入
            Iterator iterator = requests.stream().map(it -> (Supplier<Future>) () -> this.writeAndFlush(it)).iterator();
            ConcurrentUtils.executeChain(iterator, writePromise);
        } catch (Throwable e) {
            promise.tryFailure(e);
        }
        return promise;
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.close();
    }

    public static class ResponseHandler<T, R> extends ChannelInboundHandlerAdapter {

        private final Class<T> responseClass;

        private final int size;

        private final List<T> buffer;

        private final Promise<R> promise;

        public ResponseHandler(Class<T> responseClass, int size, Promise<R> promise) {
            this.responseClass = responseClass;
            this.size = size;
            this.promise = promise;
            this.buffer = new ArrayList<>(size <= 0 ? 1 : size);
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (responseClass.isAssignableFrom(msg.getClass())) {
                channelRead0(ctx, (T) msg);
            } else {
                super.channelRead(ctx, msg);
            }
        }

        private synchronized void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
            this.buffer.add(msg);
            if (this.buffer.size() >= size) {
                if (this.size <= 0) {
                    promise.setSuccess((R) this.buffer.get(0));
                } else {
                    R result = (R) new ArrayList<>(this.buffer);
                    promise.setSuccess(result);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            this.buffer.clear();
            promise.tryFailure(cause);
        }
    }
}
