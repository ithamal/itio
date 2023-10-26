package io.github.ithaml.itio.client;

import io.netty.util.concurrent.Future;

/**
 * 连接池
 *
 * @author: ken.lin
 * @since: 2023-10-20 09:41
 */
public interface ConnectionPool {

    void initialize();

    Future<Connection> getConnection();

    void returnChannel(Connection connection);

    Future shutdown();
}
