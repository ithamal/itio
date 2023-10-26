package io.github.ithaml.itio.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

/**
 * 客户端端限流
 *
 * @author: ken.lin
 * @since: 2023-09-30 21:13
 */
@FunctionalInterface
public interface ChannelHandlerProvider<T extends ChannelHandler> {

    /**
     * 获取处理器
     *
     * @param channel 通道对象
     * @return 处理器
     */
    T getHandler(Channel channel);
}

