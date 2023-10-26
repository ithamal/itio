package io.github.ithaml.itio;

import io.github.ithaml.itio.client.Connection;
import io.github.ithaml.itio.client.ItioClient;
import io.github.ithaml.itio.client.PoolSetting;
import io.github.ithaml.itio.client.impl.ItioClientImpl;
import io.github.ithaml.itio.concurrent.impl.LeakyBucketRateLimiter;
import io.github.ithaml.itio.handler.ClientSideRateLimitHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: ken.lin
 * @since: 2023-09-30 20:13
 */
public class HttpClientTest {

    @Test
    @SneakyThrows
    public void test() {
        ItioClient client = new ItioClientImpl();
//        client.setLogging(true);
        client.registerCodecHandler(ch -> new HttpClientCodec());
        client.registerCodecHandler(ch -> new HttpObjectAggregator(8192));
        client.registerCodecHandler(ch -> new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));
        client.registerRateLimitHandler(ch -> new ClientSideRateLimitHandler(new LeakyBucketRateLimiter(ch.eventLoop(),1)));
        client.registerBizHandler(ch -> new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                super.channelRead(ctx, msg);
            }
        });
        String host = "localhost";
//        String host = "www.qq.com";
        client.setAddress(host, 8081);
        client.setPoolSetting(new PoolSetting().minimumSize(1).maximumSize(1));
        client.initialize();
        try {
            int num = 10;
            Instant startTime = Instant.now();
            CountDownLatch countDownLatch = new CountDownLatch(num);
            ExecutorService executorService = Executors.newFixedThreadPool(Math.min(num, 8));
            for (int i = 0; i < num; i++) {
                Connection connection = client.getConnection().get();
                System.out.println("已连接：" + connection.isAlive() + "," + connection.getRealConnection());
                executorService.submit(() -> {
                    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
                    request.headers().set("Host", host);
                    request.headers().set("Connection", "keep-alive");
                    System.out.println("准备请求");
                    connection.writeForResponse(request, FullHttpResponse.class).addListener(future -> {
                        try{
                            System.out.println("已响应：" + (num - countDownLatch.getCount() + 1));
                            FullHttpResponse response = (FullHttpResponse) future.get();
                            System.out.println(response);
                            System.out.println(response.content().toString(StandardCharsets.UTF_8));
                            response.release();
//                            connection.getRealConnection().close().syncUninterruptibly();
                            connection.close().syncUninterruptibly();
                            System.out.println("已关闭");
                        }catch (Exception e) {
                            e.printStackTrace();
                        }finally {
                            countDownLatch.countDown();
                        }
                    });
                });
            }
            try {
                countDownLatch.await();
                System.out.println("耗时： " + Duration.between(startTime, Instant.now()).toMillis() + "ms");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.shutdown().syncUninterruptibly();
            System.out.println("已关闭");
        }
    }
}
