package io.github.ithaml.itio.client.impl;

import io.github.ithaml.itio.client.*;
import io.github.ithaml.itio.executor.DefaultEventLoops;
import io.github.ithaml.itio.handler.ChannelHandlerProvider;
import io.github.ithaml.itio.util.ConcurrentUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author: ken.lin
 * @since: 2023-10-20 10:24
 */
public class ItioClientImpl implements ItioClient, ConnectionFactory {

    private Bootstrap bootstrap;
    private Class<? extends Channel> channelClass = NioSocketChannel.class;
    private volatile boolean isInitialized;
    private final List<ChannelHandlerProvider> codecHandlerProviders = new ArrayList<>();
    private final List<ChannelHandlerProvider> bizHandlerProviders = new ArrayList<>();
    private final List<ChannelHandlerProvider> rateLimiterHandlerProviders = new ArrayList<>();
    private final HashMap<ChannelOption, Object> optionsHashMap = new HashMap<>();
    private boolean isLogging;
    private EventLoopGroup workEventLoopGroup;
    private String host;
    private int port;
    private PoolSetting poolSetting;
    private ConnectionPool connectionPool;
    private Handshake handshake;

    @Override
    public void setChannelClass(Class<? extends Channel> channelClass) {
        this.channelClass = channelClass;
    }

    @Override
    public void setLogging(boolean isLogging) {
        this.isLogging = isLogging;
    }

    @Override
    public void setWorkEventLoopGroup(EventLoopGroup workEventLoopGroup) {
        this.workEventLoopGroup = workEventLoopGroup;
    }

    @Override
    public <T> void option(ChannelOption<T> option, T value) {
        if (isInitialized) {
            throw new IllegalStateException();
        }
        optionsHashMap.put(option, value);
    }

    @Override
    public void setKeepAlive(boolean value) {
        if (isInitialized) {
            throw new IllegalStateException();
        }
        optionsHashMap.put(ChannelOption.SO_KEEPALIVE, value);
    }


    @Override
    public void registerCodecHandler(ChannelHandlerProvider provider) {
        if (isInitialized) {
            throw new IllegalStateException();
        }
        codecHandlerProviders.add(provider);
    }

    @Override
    public void registerBizHandler(ChannelHandlerProvider provider) {
        if (isInitialized) {
            throw new IllegalStateException();
        }
        bizHandlerProviders.add(provider);
    }

    @Override
    public void registerRateLimitHandler(ChannelHandlerProvider provider) {
        if (isInitialized) {
            throw new IllegalStateException();
        }
        rateLimiterHandlerProviders.add(provider);
    }

    @Override
    public void setHandShake(Handshake handshake) {
        this.handshake = handshake;
    }

    @Override
    public void setAddress(String host, int port) {
        if (isInitialized) {
            throw new IllegalStateException();
        }
        this.host = host;
        this.port = port;
    }

    @Override
    public void initialize() {
        if (workEventLoopGroup == null) {
            workEventLoopGroup = DefaultEventLoops.getWorkEventGroup();
        }
        // 创建启动器
        bootstrap = new Bootstrap();
        bootstrap.group(workEventLoopGroup).channel(channelClass);
        optionsHashMap.forEach(bootstrap::option);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                // 日志
                if (isLogging) {
                    ch.pipeline().addLast("logging", new LoggingHandler());
                }
                // 编码处理
                for (ChannelHandlerProvider provider : codecHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(ch);
                    if (handler != null) {
                        ch.pipeline().addLast(handler);
                    }
                }
                // 流控处理器
                for (ChannelHandlerProvider provider : rateLimiterHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(ch);
                    if (handler != null) {
                        ch.pipeline().addLast(handler);
                    }
                }
                ch.pipeline().addLast("noop", new ChannelHandlerAdapter() {
                });
                // 业务处理器
                for (ChannelHandlerProvider provider : bizHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(ch);
                    if (handler != null) {
                        ch.pipeline().addLast(handler);
                    }
                }
            }
        });
        // 创建连接池
        if (poolSetting != null) {
            connectionPool = new ConnectionPoolImpl(poolSetting, this, workEventLoopGroup.next());
            connectionPool.initialize();
        }
        // 初始化标识
        isInitialized = true;
    }

    @Override
    public Future<Connection> createConnection() {
        DefaultPromise<Connection> promise = new DefaultPromise<>(workEventLoopGroup.next());
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            Channel channel = future.channel();
            ConnectionImpl connection = new ConnectionImpl(channel);
            if (future.isSuccess()) {
                if (handshake != null) {
                    channel.pipeline().addBefore("noop", "handshake", new HandshakeHandler(handshake, connection, promise));
                    Object request = handshake.buildRequest();
                    channel.writeAndFlush(request);
                } else if (!promise.isCancelled()) {
                    promise.setSuccess(connection);
                }
            } else {
                promise.setFailure(future.cause());
            }
        });
        return promise;
    }


    @Override
    public Connection connect() {
        checkInitialize();
        return createConnection().syncUninterruptibly().getNow();
    }

    @Override
    public Future<Connection> bind() {
        checkInitialize();
        DefaultPromise<Connection> promise = new DefaultPromise<>(workEventLoopGroup.next());
        ChannelFuture channelFuture = host == null ? bootstrap.bind(port) : bootstrap.bind(host, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            Channel channel = future.channel();
            ConnectionImpl connection = new ConnectionImpl(channel);
            if (future.isSuccess()) {
                if (!promise.isCancelled()) {
                    promise.setSuccess(connection);
                }
            } else {
                promise.setFailure(future.cause());
            }
        });
        return promise;
    }

    @Override
    public Future<Connection> getConnection() {
        checkInitialize();
        if (connectionPool == null) {
            return createConnection();
        } else {
            return connectionPool.getConnection();
        }
    }

    @Override
    public void setPoolSetting(PoolSetting setting) {
        this.poolSetting = setting;
    }

    @Override
    public ConnectionPool getPool() {
        if (poolSetting == null) {
            throw new IllegalArgumentException("can't get pool setting");
        }
        return null;
    }

    @Override
    public Future shutdown() {
        List<Supplier<Future>> fnList = new ArrayList();
        if (connectionPool != null) {
            fnList.add(connectionPool::shutdown);
        }
        return ConcurrentUtils.executeChain(workEventLoopGroup.next(), fnList);
    }

    private void checkInitialize() {
        if (isInitialized) {
            return;
        }
        synchronized (this) {
            if (isInitialized) {
                return;
            }
            initialize();
        }
    }
}
