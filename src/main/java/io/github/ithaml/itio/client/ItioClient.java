package io.github.ithaml.itio.client;

import io.github.ithaml.itio.handler.ChannelHandlerProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;


/**
 * @author: ken.lin
 * @since: 2023-09-30 14:50
 */
public interface ItioClient {

    /**
     * 信道类，默认NIO
     * @param channelClass 信道类型
     */
    void setChannelClass(Class<? extends Channel> channelClass);

    /**
     * 是否开启日志
     */
    void setLogging(boolean isLogging);

    /**
     * IO执行器，设置核心线程数，可以设置为CPU核心数的N倍，N的取值范围为1到3
     */
    void setWorkEventLoopGroup(EventLoopGroup eventLoopGroup);

    /**
     * 配置选项
     *
     * @param option 选项
     * @param value  值
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
     * @param provider
     */
    void registerCodecHandler(ChannelHandlerProvider provider);

    /**
     * 注册业务处理器，用于异步处理
     *
     * @param provider
     */
    void registerBizHandler(ChannelHandlerProvider provider);

    /**
     * 流控处理器
     *
     * @param provider
     */
    void registerRateLimitHandler(ChannelHandlerProvider provider);

    /***
     * 设置握手类，如果设置，那么在连接时会调用
     * @param handshake
     */
    void setHandShake(Handshake handshake);

    /**
     * 地址
     *
     * @param host 主机
     * @param port 端口
     */
    void setAddress(String host, int port);

    /**
     * 初始化，如果不手动初始化，将在connect时自动初始化
     */
    void initialize();

    /**
     * 创建一个新连接（注意：此为非池化连接）
     *
     * @return
     */
    Connection connect();

    /**
     * 绑定端口，用于UDP服务模式
     *
     * @return
     */
    Future<Connection> bind();

    /**
     * 获取连接，如果存在
     *
     * @return
     */
    Future<Connection> getConnection();

    /**
     * 池化配置
     * @param setting  设置
     */
    void setPoolSetting(PoolSetting setting);

    /**
     * 获取池化
     *
     * @return
     */
    ConnectionPool getPool();

    /**
     * 停止所有连接
     */
    Future shutdown();
}
