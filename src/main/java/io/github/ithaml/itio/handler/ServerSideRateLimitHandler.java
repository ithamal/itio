package io.github.ithaml.itio.handler;

import io.github.ithaml.itio.concurrent.RateLimiter;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import lombok.Setter;

/**
 * 服务器端限流
 *
 * @author: ken.lin
 * @since: 2023-10-20 21:14
 */
public class ServerSideRateLimitHandler extends ChannelDuplexHandler {

    @Getter
    @Setter
    private RateLimiter rateLimiter;

    /**
     * 构造方法
     *
     * @param rateLimiter
     */
    public ServerSideRateLimitHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * 接收消息类型
     *
     * @param msg
     */
    protected boolean acceptMessage(Object msg) {
        return true;
    }


    /**
     * 读取
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 非接受消息类型
        if (!acceptMessage(msg)) {
            super.channelRead(ctx, msg);
            return;
        }
        // 可直接进入
        if (rateLimiter.tryAcquire()) {
            super.channelRead(ctx, msg);
            return;
        }
        // 等待进入
        rateLimiter.acquire().addListener(future -> {
            if (future.isSuccess()) {
                ctx.fireChannelRead(msg);
            } else {
                ctx.fireExceptionCaught(future.cause());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        rateLimiter.release();
        super.exceptionCaught(ctx, cause);
    }

    /**
     * 写入
     *
     * @param ctx     the {@link ChannelHandlerContext} for which the write operation is made
     * @param msg     the message to write
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (acceptMessage(msg)) {
            rateLimiter.release();
        }
        super.write(ctx, msg, promise);
    }
}
