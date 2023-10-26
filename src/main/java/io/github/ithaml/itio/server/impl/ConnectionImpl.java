package io.github.ithaml.itio.server.impl;

import io.github.ithaml.itio.server.Connection;
import io.github.ithaml.itio.server.ConnectionId;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:19
 */
class ConnectionImpl implements Connection {

    private ConnectionId id;

    private final Channel channel;

    public ConnectionImpl(ConnectionId id, Channel channel) {
        this.id = id;
        this.channel = channel;
    }

    @Override
    public ConnectionId getId() {
        return id;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isAlive() {
        return channel.isActive();
    }

    @Override
    public Future writeAndFlush(Object msg) {
        return channel.writeAndFlush(msg);
    }

    @Override
    public Future close() {
        return channel.close();
    }

    @Override
    public Future closeFuture() {
        return channel.closeFuture();
    }
}
