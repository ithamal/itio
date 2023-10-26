package io.github.ithaml.itio.server;

import io.github.ithaml.itio.handler.ChannelHandlerProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.concurrent.Future;

/**
 * @author: ken.lin
 * @since: 2023-09-30 14:50
 */
public interface ItioServer {

    /**
     * 信道类，默认NIO
     * @param channelClass 信道类型
     */
    void setChannelClass(Class<? extends ServerChannel> channelClass);

    /**
     * 是否开启日志
     * @param isLogging 是否开启
     */
    void setLogging(boolean isLogging);

    /**
     * 主执行器，建议设置为1
     * @param eventLoopGroup 执行器
     */
    void setBossEventLoopGroup(EventLoopGroup eventLoopGroup);

    /**
     * IO执行器，设置核心线程数，可以设置为CPU核心数的N倍，N的取值范围为1到3
     * @param eventLoopGroup 执行器
     */
    void setWorkEventLoopGroup(EventLoopGroup eventLoopGroup);

    /**
     * 配置项
     *
     * @param option
     * @param value
     * @param <T>
     */
    <T> void option(ChannelOption<T> option, T value);

    /**
     * 是否保留存活
     *
     * @param value
     */
    void setKeepAlive(boolean value);

    /**
     * 注册编码处理器
     *
     * @param provider 处理器
     */
    void registerCodecHandler(ChannelHandlerProvider provider);

    /**
     * 注册业务处理器，用于异步处理
     *
     * @param provider 处理器
     */
    void registerBizHandler(ChannelHandlerProvider provider);

    /**
     * 流控处理器
     *
     * @param provider
     */
    void registerRateLimitHandler(ChannelHandlerProvider provider);

    /**
     * 握手处理器, 应用有状态(长)连接程序；短连接程序不建议使用
     * @param handshake
     */
    void setHandshake(Handshake handshake);

    /**
     * 连接管理器, 应用有状态(长)连接程序；管理握手后的连接
     * @return
     */
    ConnectionManager getConnectionManger();

    /**
     * 初始化，如果不手动初始化，将在listen时自动初始化
     */
    void initialize();

    /**
     * 连接
     * @param port
     */
    Future listen(int port);


    /**
     * 停止连接
     * @return 响应
     */
    Future shutdown();
}
