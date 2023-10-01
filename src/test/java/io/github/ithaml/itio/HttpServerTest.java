package io.github.ithaml.itio;

import io.github.ithaml.itio.server.ItioServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-09-30 20:13
 */
public class HttpServerTest {

    @Test
    @SneakyThrows
    public void test() {
        ItioServer server = new ItioServer();
        server.setLogging(true);
        server.registerCodecHandler(ch -> new HttpServerCodec());
        server.registerCodecHandler(ch -> new HttpObjectAggregator(1024 * 1024));
        // 业务处理
        server.registerBizHandler(ch->new SimpleChannelInboundHandler<FullHttpRequest>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                System.out.println("客服端地址" + ctx.channel().remoteAddress());
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.content().writeBytes("Hello World".getBytes(CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                ctx.writeAndFlush(response);
            }
        });
        server.listen(8080);
        System.out.println("已启动监听");
        TimeUnit.SECONDS.sleep(30);
        server.shutdown();
        System.out.println("已停止服务");
    }
}
