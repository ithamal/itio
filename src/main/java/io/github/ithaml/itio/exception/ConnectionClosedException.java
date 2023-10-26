package io.github.ithaml.itio.exception;

/**
 * @author: ken.lin
 * @since: 2023-10-20 14:17
 */
public class ConnectionClosedException extends RuntimeException {

    public ConnectionClosedException() {
        super("connection was closed");
    }
}
