package io.github.ithaml.itio.server.impl;

import io.github.ithaml.itio.server.*;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @author: ken.lin
 * @since: 2023-10-22 21:05
 */
class HandshakeHandler extends ChannelInboundHandlerAdapter {

    private final Handshake handshake;

    private final Channel channel;

    private final ConnectionManager connectionManager;

    public HandshakeHandler(Handshake handshake, Channel channel, ConnectionManager connectionManager) {
        this.handshake = handshake;
        this.channel = channel;
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object request) throws Exception {
        if (!handshake.acceptRequest(request)) {
            super.channelRead(ctx, request);
            return;
        }
        try {
            Connection connection = createConnection(request);
            handshake.handleRequest(request).addListener((GenericFutureListener<Future<Object>>) future -> {
                try {
                    if(future.isSuccess()){
                        Object response = future.get();
                        ConnectionHolder.bind(channel, connection);
                        connectionManager.register(connection);
                        connection.writeAndFlush(response);
                    }else{
                        Throwable cause = future.cause();
                        channel.writeAndFlush(handshake.buildErrorResponse(request, cause));
                    }
                } catch (Exception e) {
                    connection.close();
                }
            });
        }catch (Throwable e){
            channel.writeAndFlush(handshake.buildErrorResponse(request, e));
        }
    }

    private Connection createConnection(Object request) {
        ConnectionId connectionId = handshake.getConnectionId(request);
        Connection connection = new ConnectionImpl(connectionId, channel);
        connection.closeFuture().addListener(future -> {
            ConnectionHolder.unbind(channel);
            connectionManager.unregister(connection);
        });
        return connection;
    }
}
