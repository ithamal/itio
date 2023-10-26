### itio
居于Netty的高性能非阻塞IO框架

### Maven 依赖
```xml
    <groupId>io.github.ithamal</groupId>
    <artifactId>itio</artifactId>
    <version>1.0.2</version>
```

### 特性
- 简化netty框架的客户端和服务器端操作
- 支持连接池、流控、握手等机制
- 支持客户端同步模式

### 客户端示例
```java
ItioClient client = new ItioClientImpl();
client.setLogging(true);
client.registerCodecHandler(ch -> new HttpClientCodec());
client.registerCodecHandler(ch -> new HttpObjectAggregator(8192));
client.registerBizHandler(ch -> new ChannelInboundHandlerAdapter() {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          super.channelRead(ctx, msg);
    }
});
client.setAddress("www.qq.com", 80);
client.setPoolSetting(new PoolSetting().minimumSize(5));
client.initialize();
Connection connection = client.getConnection();
System.out.println("已连接：" + connection.isAlive());
HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
request.headers().set("Host", "www.qq.com");
try {
    System.out.println("已请求");
    FullHttpResponse response = connection.writeForResponse(request, FullHttpResponse.class).get();
    System.out.println("已响应");
    System.out.println(response);
    System.out.println(response.content().toString(StandardCharsets.UTF_8));
    response.release();
} finally {
    connection.close().syncUninterruptibly();
    client.shutdown(10, TimeUnit.SECONDS);
    System.out.println("已关闭");
}
```

### 服务器端示例
```java
ItioServer server = new ItioServerImpl();
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
```
