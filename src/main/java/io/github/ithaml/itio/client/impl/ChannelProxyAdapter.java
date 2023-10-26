package io.github.ithaml.itio.client.impl;

import io.github.ithaml.itio.client.Connection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;

import java.util.Collection;

/**
 * @author: ken.lin
 * @since: 2023-10-20 14:21
 */
public class ChannelProxyAdapter implements Connection {

    private final Connection connection;

    public Connection getRealConnection() {
        return connection;
    }

    @Override
    public boolean isAlive() {
        return connection.isAlive();
    }

    @Override
    public <R> Future<R> writeForResponse(Object request, Class<R> responseClass) {
        return connection.writeForResponse(request, responseClass);
    }

    @Override
    public <R> Future<Collection<R>> writeForResponses(Collection requests, Class<R> responseClass) {
        return connection.writeForResponses(requests, responseClass);
    }

    @Override
    public <R> Future<Collection<R>> writeForResponses(Collection requests, Class<R> responseClass, int size) {
        return connection.writeForResponses(requests, responseClass, size);
    }

    @Override
    public void setAttribute(String name, Object value) {
        connection.setAttribute(name, value);
    }

    @Override
    public <V> V getAttribute(String name) {
        return connection.getAttribute(name);
    }

    @Override
    public Future closeFuture() {
        return connection.closeFuture();
    }

    public ChannelProxyAdapter(Connection connection) {
        this.connection = connection;
    }

    public Channel getChannel() {
        return connection.getChannel();
    }

    @Override
    public ChannelFuture close() {
        return connection.close();
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return connection.writeAndFlush(msg);
    }
}
