package io.github.ithaml.itio.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:16
 */
public interface Connection {

    /**
     * 连接ID
     * @return
     */
    ConnectionId getId();

    /**
     * 通道，不推荐使用，会引发不安全操作
     * @return
     */
    Channel getChannel();

    /**
     * 远程地址
     *
     * @return
     */
    SocketAddress remoteAddress();

    /**
     * 是否存活
     * @return
     */
    boolean isAlive();

    /**
     * 写入消息
     * @param msg
     */
    Future writeAndFlush(Object msg);

    /**
     * 关闭
     */
    Future close();

    /**
     * 关闭回调
     * @return
     */
    Future closeFuture();
}
