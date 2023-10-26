package io.github.ithaml.itio.server.impl;

import io.github.ithaml.itio.executor.DefaultEventLoops;
import io.github.ithaml.itio.handler.ChannelHandlerProvider;
import io.github.ithaml.itio.server.*;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: ken.lin
 * @since: 2023-09-30 14:50
 */
public class ItioServerImpl implements ItioServer {

    private Channel serverChannel;
    private EventLoopGroup bossEventLoopGroup;
    private EventLoopGroup workEventLoopGroup;
    private ServerBootstrap bootstrap;
    private Handshake handshake;
    private Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
    private volatile boolean isInitialized;
    private final Map<ChannelOption, Object> optionsMap = new HashMap<>();
    private final List<ChannelHandlerProvider> codecHandlerProviders = new ArrayList<>();
    private final List<ChannelHandlerProvider> bizHandlerProviders = new ArrayList<>();
    private final List<ChannelHandlerProvider> rateLimiterHandlerProviders = new ArrayList<>();
    private final ConnectionManager connectionManager = new ConnectionManagerImpl();

    /**
     * 是否开启日志
     */
    @Getter
    @Setter
    private boolean isLogging;

    @Override
    public void setChannelClass(Class<? extends ServerChannel> channelClass) {
        this.channelClass = channelClass;
    }

    @Override
    public void setBossEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.bossEventLoopGroup = eventLoopGroup;
    }

    @Override
    public void setWorkEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.workEventLoopGroup = eventLoopGroup;
    }

    /**
     * 设置选项参数
     *
     * @param option 选项参数
     * @param value  参数值
     * @param <T>    参数值类型
     */
    public <T> void option(ChannelOption<T> option, T value) {
        optionsMap.put(option, value);
    }

    @Override
    public void setKeepAlive(boolean value) {
        optionsMap.put(ChannelOption.SO_KEEPALIVE, value);
    }

    /**
     * 注册编码处理器，按顺序添加到最后
     *
     * @param provider 处理器提供者
     */
    public void registerCodecHandler(ChannelHandlerProvider provider) {
        codecHandlerProviders.add(provider);
    }


    /**
     * 注册业务处理器，按顺序添加到最后
     *
     * @param provider 处理器提供者
     */
    public void registerBizHandler(ChannelHandlerProvider provider) {
        bizHandlerProviders.add(provider);
    }

    @Override
    public void registerRateLimitHandler(ChannelHandlerProvider provider) {
        rateLimiterHandlerProviders.add(provider);
    }

    @Override
    public void setHandshake(Handshake handshake) {
        this.handshake = handshake;
    }

    @Override
    public ConnectionManager getConnectionManger() {
        return connectionManager;
    }

    @Override
    public void initialize() {
        if (bossEventLoopGroup == null) {
            bossEventLoopGroup = DefaultEventLoops.getBossEventGroup();
        }
        if (workEventLoopGroup == null) {
            workEventLoopGroup = DefaultEventLoops.getWorkEventGroup();
        }
        ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                // 日志
                if (isLogging) {
                    channel.pipeline().addLast("logging", new LoggingHandler());
                }
                // 编码处理
                for (ChannelHandlerProvider<?> provider : codecHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(channel);
                    if (handler != null) {
                        channel.pipeline().addLast(handler);
                    }
                }
                // 流控处理器
                for (ChannelHandlerProvider provider : rateLimiterHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(channel);
                    if (handler != null) {
                        channel.pipeline().addLast(handler);
                    }
                }
                // 需握手机制
                if (handshake != null) {
                    channel.pipeline().addLast(new HandshakeHandler(handshake, channel, connectionManager));
                }
                // 业务处理器
                for (ChannelHandlerProvider provider : bizHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(channel);
                    if (handler != null) {
                        channel.pipeline().addLast(handler);
                    }
                }
            }
        };
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossEventLoopGroup, workEventLoopGroup).channel(channelClass);
        bootstrap.childHandler(channelInitializer);
        optionsMap.forEach(bootstrap::option);
    }

    /**
     * 开始监听
     *
     * @param port 端口
     */
    public Future listen(int port) {
        checkInitialize();
        ChannelFuture channelFuture = bootstrap.bind(port);
        serverChannel = channelFuture.channel();
        return channelFuture;
    }

    @Override
    public Future shutdown() {
        return serverChannel.close();
    }

    private void checkInitialize() {
        if (isInitialized) {
            return;
        }
        synchronized (this) {
            if (!isInitialized) {
                initialize();
            }
        }
    }
}
