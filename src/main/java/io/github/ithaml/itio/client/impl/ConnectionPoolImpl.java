package io.github.ithaml.itio.client.impl;

import io.github.ithaml.itio.client.Connection;
import io.github.ithaml.itio.client.ConnectionFactory;
import io.github.ithaml.itio.client.ConnectionPool;
import io.github.ithaml.itio.client.PoolSetting;
import io.github.ithaml.itio.util.ConcurrentUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 连接池
 *
 * @author: ken.lin
 * @since: 2023-10-20 09:41
 */
public class ConnectionPoolImpl implements ConnectionPool {

    private final static Logger logger = LoggerFactory.getLogger(ConnectionPoolImpl.class);

    private final PoolSetting setting;
    private final ConnectionFactory connectionFactory;
    private final AtomicInteger counter = new AtomicInteger();
    private final LinkedBlockingQueue<Connection> freeConnections;
    private final LinkedBlockingQueue<Promise> waitPromises;
    private final EventExecutor eventExecutor;
    private volatile boolean isShutdown = false;

    public ConnectionPoolImpl(PoolSetting setting, ConnectionFactory connectionFactory, EventExecutor eventExecutor) {
        this.connectionFactory = connectionFactory;
        this.setting = setting;
        this.eventExecutor = eventExecutor;
        this.freeConnections = new LinkedBlockingQueue<>();
        this.waitPromises = new LinkedBlockingQueue<>();
    }

    @Override
    public void initialize() {
        ensureMinimumConnections();
        // 创建清理任务
        int period = setting.getKeepAliveTime() > 0 ? setting.getKeepAliveTime() : 10;
        eventExecutor.scheduleAtFixedRate(new CleanRunnable(), period, period, TimeUnit.SECONDS);
    }

    @Override
    public Future<Connection> getConnection() {
        DefaultPromise<Connection> promise = new DefaultPromise<>(eventExecutor);
        Connection connection = freeConnections.poll();
        if (connection != null) {
            promise.setSuccess(connection);
        } else {
            waitPromises.add(promise);
            tryCreateConnection();
        }
        return promise;
    }

    @Override
    public void returnChannel(Connection connection) {
        // 真实连接
        connection = connection.getRealConnection();
        // 重新加入连接
        addConnection(connection);
    }

    @Override
    public Future shutdown() {
        List<Supplier<Future>> fnList = new ArrayList<>(freeConnections.size());
        Connection connection;
        while ((connection = freeConnections.poll()) != null) {
            fnList.add(connection::close);
        }
        return ConcurrentUtils.executeChain(eventExecutor, fnList);
    }

    private void closeConnection(Connection connection) {
        counter.decrementAndGet();
        connection.close();
    }

    private void addConnection(Connection connection) {
        // 非存活状态
        if (!connection.isAlive()) {
            closeConnection(connection);
            return;
        }
        //存活时间
        ((ConnectionImpl) connection).checkAliveTime.set(Instant.now());
        // 有等待的promise
        Promise promise = waitPromises.poll();
        if (promise != null && !promise.isCancelled() && !promise.isDone()) {
            promise.setSuccess(new AutoReturnChannelProxy(connection));
        } else {
            freeConnections.add(connection);
        }
    }

    private boolean tryCreateConnection() {
        if (counter.get() >= setting.getMinimumSize()) {
            return false;
        }
        if (counter.incrementAndGet() > setting.getMaximumSize()) {
            counter.decrementAndGet();
            return false;
        }
        connectionFactory.createConnection().addListener(future -> {
            Connection connection = (Connection) future.getNow();
            if (future.isSuccess()) {
                // 防止连接泄露
                connection.closeFuture().addListener((ChannelFutureListener) f -> {
                    closeConnection(connection);
                    ensureMinimumConnections();
                });
                addConnection(connection);
            } else{
                logger.error("Failed to establish a new connection, {}" , future.cause().getMessage());
                counter.decrementAndGet();
                if (connection != null) {
                    closeConnection(connection);
                }
            }
        });
        return true;
    }

    /**
     * 保持最小连接数
     */
    private void ensureMinimumConnections() {
        for (int i = counter.get(); i < setting.getMinimumSize(); i++) {
            tryCreateConnection();
        }
    }

    private class CleanRunnable implements Runnable {

        @Override
        public void run() {
            int size = freeConnections.size();
            for (int i = 0; i < size; i++) {
                Connection connection = freeConnections.poll();
                if (connection == null) {
                    break;
                }
                if (isCloseable(connection)) {
                    closeConnection(connection);
                } else {
                    freeConnections.add(connection);
                }
            }
            ensureMinimumConnections();
        }

        private boolean isCloseable(Connection connection) {
            if (!connection.isAlive()) {
                return true;
            }
            if (setting.getKeepAliveTime() <= 0) {
                return false;
            }
            if (!(connection instanceof ConnectionImpl)) {
                return false;
            }
            Duration between = Duration.between(((ConnectionImpl) connection).checkAliveTime.get(), Instant.now());
            return between.getSeconds() > setting.getKeepAliveTime();
        }
    }

    private class AutoReturnChannelProxy extends ChannelProxyAdapter {

        public AutoReturnChannelProxy(Connection connection) {
            super(connection);
        }

        @Override
        public ChannelFuture close() {
            ConnectionPoolImpl.this.returnChannel(this);
            return new DefaultChannelPromise(getChannel()).setSuccess();
        }
    }
}
