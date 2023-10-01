package io.github.ithaml.itio.handler.provider;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

/**
 * @author: ken.lin
 * @since: 2023-09-30 21:13
 */
public class FixedChannelHandlerProvider implements ChannelHandlerProvider {

    private final ChannelHandler handler;

    public FixedChannelHandlerProvider(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    public ChannelHandler getHandler(Channel channel) {
        return handler;
    }
}
