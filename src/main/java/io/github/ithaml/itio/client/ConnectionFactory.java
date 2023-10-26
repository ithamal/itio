package io.github.ithaml.itio.client;

import io.netty.util.concurrent.Future;

/**
 * 连接工厂
 *
 * @author: ken.lin
 * @since: 2023-10-22 20:05
 */
public interface ConnectionFactory {

    /**
     * 创建连接
     *
     * @return
     */
    Future<Connection> createConnection();
}
