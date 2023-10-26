package io.github.ithaml.itio.server;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:51
 */
public class ConnectionHolder {

    private final static AttributeKey<Connection> CONNECTION_ATTR_KEY = AttributeKey.newInstance("connection");

    public static void bind(Channel channel, Connection connection){
         channel.attr(CONNECTION_ATTR_KEY).set(connection);
    }

    public static void unbind(Channel channel){
        channel.attr(CONNECTION_ATTR_KEY).set(null);
    }

    public static Connection getConnection(Channel channel){
        return channel.attr(CONNECTION_ATTR_KEY).get();
    }
}
