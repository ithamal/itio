package io.github.ithaml.itio;

import com.jayway.jsonpath.JsonPath;
import io.github.ithaml.itio.concurrent.impl.LeakyBucketRateLimiter;
import io.github.ithaml.itio.handler.ServerAfterHandshakeHandler;
import io.github.ithaml.itio.handler.ServerSideRateLimitHandler;
import io.github.ithaml.itio.server.Connection;
import io.github.ithaml.itio.server.ConnectionId;
import io.github.ithaml.itio.server.Handshake;
import io.github.ithaml.itio.server.ItioServer;
import io.github.ithaml.itio.server.impl.ItioServerImpl;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-09-30 20:13
 */
public class HttpServerAuthTest {

    @Test
    @SneakyThrows
    public void test() {
        ItioServer server = new ItioServerImpl();
//        server.setLogging(true);
        server.registerCodecHandler(ch -> new HttpServerCodec());
        server.registerCodecHandler(ch -> new HttpObjectAggregator(1024 * 1024));
        server.registerRateLimitHandler(ch -> new ServerSideRateLimitHandler(new LeakyBucketRateLimiter(ch.eventLoop(), 1)));
        server.setHandshake(new Handshake<FullHttpRequest, FullHttpResponse>() {
            @Override
            public boolean acceptRequest(Object request) {
                if (!(request instanceof FullHttpRequest)) {
                    return false;
                }
                return ((FullHttpRequest) request).uri().equals("/auth");
            }

            @Override
            public ConnectionId getConnectionId(FullHttpRequest request) {
                String json = request.content().toString(StandardCharsets.UTF_8);
                String userName = JsonPath.read(json, "$.userName");
                String sessionId = UUID.randomUUID().toString();
                return new ConnectionId(sessionId, userName);
            }

            @Override
            public Future<FullHttpResponse> handleRequest(FullHttpRequest request) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.content().writeBytes("OK".getBytes(CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                return ImmediateEventExecutor.INSTANCE.<FullHttpResponse>newPromise().setSuccess(response);
            }

            @Override
            public Object buildErrorResponse(FullHttpRequest request, Throwable cause) {
                cause.printStackTrace();
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.content().writeBytes(cause.toString().getBytes(CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                return response;
            }
        });
        // 业务处理
        server.registerBizHandler(ch -> new ServerAfterHandshakeHandler<FullHttpRequest>() {

            @Override
            public void channelRead(Connection connection, FullHttpRequest request) {
                System.out.println("客服端地址" + connection.remoteAddress());
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.content().writeBytes("Hello World".getBytes(CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                connection.writeAndFlush(response);
            }

            @Override
            public Object buildUnAuthResponse(FullHttpRequest request) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
                response.content().writeBytes("UnAuthorized".getBytes(CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                return response;
            }
        });
        server.listen(8081).syncUninterruptibly();
        System.out.println("已启动监听");
        TimeUnit.SECONDS.sleep(1000);
        server.shutdown().syncUninterruptibly();
        System.out.println("已停止服务");
    }
}
