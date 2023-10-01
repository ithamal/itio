package io.github.ithaml.itio;

import io.github.ithaml.itio.client.ItioClient;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-09-30 20:13
 */
public class HttpsClientTest {

    @Test
    @SneakyThrows
    public void test(){
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        ItioClient client = new ItioClient();
        client.setLogging(true);
        client.registerCodecHandler(ch-> sslContext.newHandler(ch.alloc()));
        client.registerCodecHandler(new HttpClientCodec());
        client.registerCodecHandler(new HttpObjectAggregator(1024 * 1024));
        client.connect("www.qq.com", 443);
        System.out.println("已连接");
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().set(HttpHeaderNames.HOST, "www.qq.com");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        client.writeAndFlush(request).sync();
        System.out.println("已请求");
        try {
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
