package io.github.ithaml.itio.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;

import java.util.Collection;

/**
 * @author: ken.lin
 * @since: 2023-10-20 09:00
 */
public interface Connection {

    /**
     * 获取通道（不推荐，可能引起不安全操作）
     *
     * @return
     */
    Channel getChannel();

    /**
     * 关闭
     *
     * @return
     */
    ChannelFuture close();

    /**
     * 写入消息
     *
     * @param request
     * @return
     */
    ChannelFuture writeAndFlush(Object request);

    /**
     * 获取真实连接, 可能引发不安全操作
     *
     * @return
     */
    Connection getRealConnection();

    /**
     * 是否活跃状态
     *
     * @return
     */
    boolean isAlive();

    /**
     * 写入并等待响应
     *
     * @param request
     * @param responseClass
     * @param <R>
     * @return
     */
    <R> Future<R> writeForResponse(Object request, Class<R> responseClass);

    /**
     * 写入并等待响应
     *
     * @param requests
     * @param responseClass
     * @param <R>
     * @return
     */
    <R> Future<Collection<R>> writeForResponses(Collection requests, Class<R> responseClass);

    /**
     * 写入并等待响应
     *
     * @param requests
     * @param responseClass
     * @param size
     * @param <R>
     * @return
     */
    <R> Future<Collection<R>> writeForResponses(Collection requests, Class<R> responseClass, int size);

    /**
     * 设置属性
     *
     * @param name
     * @param value
     */
    void setAttribute(String name, Object value);

    /**
     * 获取属性
     *
     * @param name
     * @return
     */
    <V> V getAttribute(String name);

    /**
     * 关闭监听
     *
     * @return
     */
    Future closeFuture();
}
