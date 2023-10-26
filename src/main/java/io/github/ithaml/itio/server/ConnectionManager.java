package io.github.ithaml.itio.server;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:16
 */
public interface ConnectionManager {

    void register(Connection connection);

    void unregister(Connection connection);

    ConnectionList getByUserName(String userName);
}
