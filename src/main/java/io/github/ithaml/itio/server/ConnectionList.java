package io.github.ithaml.itio.server;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: ken.lin
 * @since: 2023-10-24 15:17
 */
public interface ConnectionList {

    void add(Connection connection);

    void remove(Connection connection);
}
