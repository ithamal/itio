### Maven 依赖
```xml
    <groupId>io.github.ithamal</groupId>
    <artifactId>itio</artifactId>
    <version>1.0.0</version>
```

### 特性
- 简化netty框架的客户端和服务器端操作
- 支持客户端同步读取消息

### 客户端示例
```java
 ItioClient client = new ItioClient();
client.setLogging(true);
client.registerCodecHandler(new HttpClientCodec());
client.registerCodecHandler(new HttpObjectAggregator(8192));
client.connect("www.qq.com", 80);
System.out.println("已连接");
HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
request.headers().set("Host", "www.qq.com");
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
```

### 服务器端示例
```java
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
```
