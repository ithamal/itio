package io.github.ithaml.itio.client.impl;

import io.github.ithaml.itio.client.Connection;
import io.github.ithaml.itio.client.Handshake;
import io.github.ithaml.itio.exception.HandshakeException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

import java.lang.reflect.ParameterizedType;

/**
 * @author: ken.lin
 * @since: 2023-10-22 21:05
 */
class HandshakeHandler extends ChannelInboundHandlerAdapter {

    private final Handshake handshake;

    private final Connection connection;

    private final Promise<Connection> promise;

    public HandshakeHandler(Handshake handshake, Connection connection, Promise<Connection> promise) {
        this.handshake = handshake;
        this.connection = connection;
        this.promise = promise;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!handshake.acceptResponse(msg)) {
            super.channelRead(ctx, msg);
            return;
        }
        try {
            if (promise.isCancelled()) {
                return;
            }
            handshake.handleResponse(connection, msg);
            promise.setSuccess(connection);
        } catch (HandshakeException e) {
            promise.setFailure(e);
        } finally {
            connection.getChannel().pipeline().remove(this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        promise.setFailure(cause);
    }
}
