package io.github.ithaml.itio.server.impl;

import io.github.ithaml.itio.server.Connection;
import io.github.ithaml.itio.server.ConnectionList;
import io.github.ithaml.itio.server.ConnectionManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:27
 */
class ConnectionManagerImpl implements ConnectionManager {

    private final ConcurrentHashMap<String, ConnectionList> userConnectionsHashMap = new ConcurrentHashMap<>();

    @Override
    public void register(Connection connection) {
        String userKey = connection.getId().getUserName();
        userConnectionsHashMap.computeIfAbsent(userKey, k -> new ConnectionListImpl()).add(connection);
    }

    @Override
    public void unregister(Connection connection) {
        String userKey = connection.getId().getUserName();
        userConnectionsHashMap.computeIfAbsent(userKey, k -> new ConnectionListImpl()).remove(connection);
    }

    @Override
    public ConnectionList getByUserName(String userKey) {
        return userConnectionsHashMap.get(userKey);
    }
}
