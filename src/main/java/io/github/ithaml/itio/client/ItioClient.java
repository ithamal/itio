package io.github.ithaml.itio.client;

import io.github.ithaml.itio.context.MsgLatch;
import io.github.ithaml.itio.handler.MsgLatchHandler;
import io.github.ithaml.itio.handler.provider.ChannelHandlerProvider;
import io.github.ithaml.itio.handler.provider.FixedChannelHandlerProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-09-30 14:50
 */
public class ItioClient {

    private Channel channel;

    private EventLoopGroup workerGroup;

    private final List<ChannelHandlerProvider> codecHandlerProviders = new ArrayList<>();

    private final List<ChannelHandlerProvider> bizHandlerProviders = new ArrayList<>();

    private final MsgLatch msgLatch = new MsgLatch();

    private final Map<ChannelOption, Object> optionsMap = new HashMap<>();

    /**
     * 是否开启日志
     */
    @Getter
    @Setter
    private boolean isLogging;

    /**
     * 设置核心线程数，可以设置为CPU核心数的N倍，N的取值范围为1到3
     */
    @Getter @Setter
    private int ioThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 获取通道对象
     * @return 通道对象
     */
    public Channel getChannel() {
        return channel;
    }

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
     * 设置长时间保持连接
     */
    public void setKeepAlive() {
        optionsMap.put(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * 注册编码处理器，按顺序添加到最后
     * @param handler 处理器
     */
    public void registerCodecHandler(ChannelHandler handler) {
        codecHandlerProviders.add(new FixedChannelHandlerProvider(handler));
    }


    /**
     * 注册编码处理器，按顺序添加到最后
     * @param provider 处理器提供者
     */
    public void registerCodecHandler(ChannelHandlerProvider provider) {
        codecHandlerProviders.add(provider);
    }



    /**
     * 注册业务处理器，按顺序添加到最后
     * @param handler 处理器
     */
    public void registerBizHandler(ChannelHandler handler) {
        bizHandlerProviders.add(new FixedChannelHandlerProvider(handler));
    }


    /**
     * 注册业务处理器，按顺序添加到最后
     * @param provider 处理器提供者
     */
    public void registerBizHandler(ChannelHandlerProvider provider) {
        bizHandlerProviders.add(provider);
    }

    /**
     * 连接
     * @param host 主机
     * @param port 端口
     */
    public void connect(String host, int port) {
        workerGroup = new NioEventLoopGroup(ioThreads);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class);
        optionsMap.forEach(bootstrap::option);
        bootstrap.handler(new ChannelInitializer<Channel>() {
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
                // 消息门塞
                ch.pipeline().addLast(new MsgLatchHandler(msgLatch));
                // 业务处理器
                for (ChannelHandlerProvider provider : bizHandlerProviders) {
                    ChannelHandler handler = provider.getHandler(ch);
                    if (handler != null) {
                        ch.pipeline().addLast(handler);
                    }
                }

            }
        });
        channel = bootstrap.connect(host, port).syncUninterruptibly().channel();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            channel.close().syncUninterruptibly();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 写入消息
     * @param msg 消息
     * @return 响应回调
     */
    public ChannelFuture writeAndFlush(Object msg) {
        return channel.writeAndFlush(msg);
    }

    /**
     * 同步等待结果
     *
     * @param msgClass 消息类型
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param <T>      类型
     * @return 如果超时未返回，则返回null
     */
    public <T> T waitForResponse(Class<T> msgClass, int timeout, TimeUnit unit) {
        return msgLatch.await(msgClass, timeout, unit);
    }

    /**
     * 一直等待响应
     * @param msgClass 消息类型
     * @return 对应类型消息
     * @param <T> 类型
     */
    public <T> T waitForResponse(Class<T> msgClass) {
        return msgLatch.await(msgClass, 0, TimeUnit.SECONDS);
    }
}
