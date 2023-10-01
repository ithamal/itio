package io.github.ithaml.itio.handler.provider;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

/**
 * @author: ken.lin
 * @since: 2023-09-30 21:13
 */
@FunctionalInterface
public interface ChannelHandlerProvider {
    /**
     * 获取处理器
     * @param channel 通道对象
     * @return 处理器
     */
    ChannelHandler getHandler(Channel channel);
}

