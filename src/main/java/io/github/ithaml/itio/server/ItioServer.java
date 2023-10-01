package io.github.ithaml.itio.server;

import io.github.ithaml.itio.handler.provider.ChannelHandlerProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
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
public class ItioServer {

    private Channel channel;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private final Map<ChannelOption, Object> optionsMap = new HashMap<>();

    private final List<ChannelHandlerProvider> codecHandlerProviders = new ArrayList<>();

    private final List<ChannelHandlerProvider> bizHandlerProviders = new ArrayList<>();

    /**
     * 是否开启日志
     */
    @Getter
    @Setter
    private boolean isLogging;

    /**
     * 设置IO线程数量
     */
    @Getter @Setter
    private int ioThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 设置选项参数
     * @param option 选项参数
     * @param value 参数值
     * @param <T> 参数值类型
     */
    public <T> void option(ChannelOption<T> option, T value) {
        optionsMap.put(option, value);
    }

    /**
     * 长时间保持连接
     */
    public void setKeepAlive() {
        optionsMap.put(ChannelOption.SO_KEEPALIVE, true);
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


    /**
     * 开始监听
     *
     * @param port 端口
     */
    public void listen(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(ioThreads);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
        optionsMap.forEach(bootstrap::option);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                // 编码处理
                for (ChannelHandlerProvider provider : codecHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(ch);
                    if (handler != null) {
                        ch.pipeline().addLast(handler);
                    }
                }
                // 日志
                if (isLogging) {
                    ch.pipeline().addLast(new LoggingHandler());
                }
                // 业务处理器
                for (ChannelHandlerProvider provider : bizHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(ch);
                    if (handler != null) {
                        ch.pipeline().addLast(handler);
                    }
                }
            }
        });
        channel = bootstrap.bind(port).syncUninterruptibly().channel();
    }

    /**
     *  停止监听
     */
    public void shutdown() {
        try {
            channel.close().syncUninterruptibly();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
