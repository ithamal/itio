package io.github.ithaml.itio.client;


import java.lang.reflect.ParameterizedType;

/**
 * @author: ken.lin
 * @since: 2023-10-24 16:09
 */
public abstract class HandshakeAdapter<REQ, RES> implements Handshake<REQ, RES> {

    @Override
    public boolean acceptResponse(Object response) {
        Class aClass = (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
        return aClass.isAssignableFrom(response.getClass());
    }
}
