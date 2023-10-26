package io.github.ithaml.itio.server;

import lombok.Getter;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:21
 */
@Getter
public class ConnectionId {

    private final String id;

    private final String userName;

    public ConnectionId(String id, String userName) {
        this.id = id;
        this.userName = userName;
    }
}
