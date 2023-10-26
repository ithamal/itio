package io.github.ithaml.itio.server;

import io.netty.util.concurrent.Future;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:34
 */
public interface Handshake<REQ, RES> {

    boolean acceptRequest(Object request);

    ConnectionId getConnectionId(REQ request);

    Future<RES> handleRequest(REQ request);

    Object buildErrorResponse(REQ request, Throwable cause);
}
