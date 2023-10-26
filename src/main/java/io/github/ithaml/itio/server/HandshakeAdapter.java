package io.github.ithaml.itio.server;

import java.lang.reflect.ParameterizedType;

/**
 * @author: ken.lin
 * @since: 2023-10-24 16:09
 */
public abstract class HandshakeAdapter<REQ, RES> implements Handshake<REQ, RES> {

    @Override
    public boolean acceptRequest(Object request) {
        Class aClass = (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return aClass.isAssignableFrom(request.getClass());
    }
}
