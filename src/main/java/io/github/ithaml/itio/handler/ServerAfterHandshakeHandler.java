package io.github.ithaml.itio.handler;

import io.github.ithaml.itio.server.Connection;
import io.github.ithaml.itio.server.ConnectionHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 握手后处理器
 *
 * @author: ken.lin
 * @since: 2023-10-24 16:02
 */
public abstract class ServerAfterHandshakeHandler<I> extends SimpleChannelInboundHandler<I> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, I request) throws Exception {
        Connection connection = ConnectionHolder.getConnection(ctx.channel());
        if (connection == null) {
            Object response = buildUnAuthResponse(request);
            if (response != null) {
                ctx.writeAndFlush(response);
            } else {
                super.exceptionCaught(ctx, new NullPointerException());
            }
        } else {
            channelRead(connection, request);
        }
    }

    public abstract void channelRead(Connection connection, I request);

    public abstract Object buildUnAuthResponse(I request);
}
