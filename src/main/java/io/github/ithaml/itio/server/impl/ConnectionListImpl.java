package io.github.ithaml.itio.server.impl;

import io.github.ithaml.itio.server.Connection;
import io.github.ithaml.itio.server.ConnectionList;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:30
 */
class ConnectionListImpl implements ConnectionList {

    private final List<Connection> connections= new ArrayList<>(1);

    @Override
    public synchronized void add(Connection connection){
        connections.add(connection);
    }

    @Override
    public synchronized void remove(Connection connection){
        connections.remove(connection);
    }
}
