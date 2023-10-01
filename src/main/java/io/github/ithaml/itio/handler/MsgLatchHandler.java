package io.github.ithaml.itio.handler;

import io.github.ithaml.itio.context.MsgLatch;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: ken.lin
 * @since: 2023-09-30 18:35C
 */
public class MsgLatchHandler extends ChannelInboundHandlerAdapter {

    private final static Logger logger = LoggerFactory.getLogger(MsgLatchHandler.class);

    private final MsgLatch msgLatch;

    public MsgLatchHandler(MsgLatch msgLatch) {
        this.msgLatch = msgLatch;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!msgLatch.signal(msg)) {
//            logger.warn("Can't match msg type ： {}", msg.getClass());
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        msgLatch.signal(cause);
        super.exceptionCaught(ctx, cause);
    }
}
