package io.github.ithaml.itio.client;

import io.github.ithaml.itio.exception.HandshakeException;

/**
 * 握手类
 *
 * @author: ken.lin
 * @since: 2023-10-20 14:08
 */
public interface Handshake<REQ, RES> {

    /**
     * 是否接受响应消息
     * @param response
     * @return
     */
    boolean acceptResponse(Object response);

    /**
     * 构建请求
     *
     * @return
     */
    REQ buildRequest();

    /**
     * 处理响应
     *
     * @param connection
     * @param response
     * @throws HandshakeException
     */
    void handleResponse(Connection connection, RES response) throws HandshakeException;
}
