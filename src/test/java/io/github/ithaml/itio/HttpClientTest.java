package io.github.ithaml.itio;

import io.github.ithaml.itio.client.ItioClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-09-30 20:13
 */
public class HttpClientTest {

    @Test
    @SneakyThrows
    public void test(){
        ItioClient client = new ItioClient();
        client.setLogging(true);
        client.registerCodecHandler(new HttpClientCodec());
        client.registerCodecHandler(new HttpObjectAggregator(8192));
        client.registerBizHandler(ch->new ChannelInboundHandlerAdapter(){
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                super.channelRead(ctx, msg);
            }
        });
        client.connect("www.qq.com", 80);
        System.out.println("已连接");
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
        request.headers().set("Host", "www.qq.com");
        client.writeAndFlush(request).sync();
        System.out.println("已请求");
        try {
//        Integer response = client.waitForResponse(Integer.class, 5, TimeUnit.SECONDS);
            FullHttpResponse response = client.waitForResponse(FullHttpResponse.class, 10, TimeUnit.SECONDS);
            System.out.println(response);
            System.out.println(response.content().toString(StandardCharsets.UTF_8));
            response.release();
        } finally {
            client.disconnect();
            System.out.println("已关闭");
        }
    }
}
