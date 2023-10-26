package io.github.ithaml.itio;

import io.github.ithaml.itio.client.Connection;
import io.github.ithaml.itio.client.ItioClient;
import io.github.ithaml.itio.client.impl.ItioClientImpl;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * @author: ken.lin
 * @since: 2023-09-30 20:13
 */
public class HttpsClientTest {

    @Test
    @SneakyThrows
    public void test() {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        ItioClient client = new ItioClientImpl();
        client.setLogging(true);
        client.registerCodecHandler(ch -> sslContext.newHandler(ch.alloc()));
        client.registerCodecHandler(ch -> new HttpClientCodec());
        client.registerCodecHandler(ch -> new HttpObjectAggregator(1024 * 1024));
        client.setAddress("www.qq.com", 443);
        System.out.println("已连接");
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().set(HttpHeaderNames.HOST, "www.qq.com");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        Connection connection = client.getConnection().get();
        try {
            System.out.println("已请求");
            FullHttpResponse response = connection.writeForResponse(request, FullHttpResponse.class).get();
            System.out.println(response);
            System.out.println(response.content().toString(StandardCharsets.UTF_8));
            response.release();
        } finally {
            connection.close().syncUninterruptibly();
            client.shutdown().syncUninterruptibly();
            System.out.println("已关闭");
        }
    }
}
