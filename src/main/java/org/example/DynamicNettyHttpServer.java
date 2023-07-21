package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;

import java.util.HashMap;
import java.util.Map;

public class DynamicNettyHttpServer {

    private static final Map<String, RequestHandler> routeHandlers = new HashMap<>();

    public static void main(String[] args) throws Exception {
        port(8001);
        port(8002);
    }

    private static void port(int port) throws InterruptedException {
        DynamicNettyHttpServer server = new DynamicNettyHttpServer();
        server.addRoute("/test", new TestRequestHandler());
        server.start(port);
    }

    public void addRoute(String path, RequestHandler handler) {
        routeHandlers.put(path, handler);
    }

    public void start(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(new DynamicHttpRequestHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("Server started on port " + port);

            future.channel().closeFuture().sync();
        } finally {
            Future<?> bossShutdownFuture = bossGroup.shutdownGracefully();
            Future<?> workerShutdownFuture = workerGroup.shutdownGracefully();

            bossShutdownFuture.sync();
            workerShutdownFuture.sync();
        }
    }

    interface RequestHandler {
        FullHttpResponse handleRequest(FullHttpRequest request);
    }

    static class DynamicHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final HttpResponseStatus NOT_FOUND_STATUS = HttpResponseStatus.NOT_FOUND;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String uri = request.uri();
            RequestHandler handler = findHandler(uri);
            FullHttpResponse response;

            if (handler != null) {
                response = handler.handleRequest(request);
            } else {
                response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, NOT_FOUND_STATUS);
            }

            // Write and flush the response to the client.
            ctx.writeAndFlush(response);
        }

        private RequestHandler findHandler(String uri) {
            return DynamicNettyHttpServer.routeHandlers.get(uri);
        }
    }

    static class TestRequestHandler implements RequestHandler {
        @Override
        public FullHttpResponse handleRequest(FullHttpRequest request) {
            String content = "Hello, Netty Dynamic Server!";
            return createHttpResponse(HttpResponseStatus.OK, content);
        }
    }

    static class AnotherRequestHandler implements RequestHandler {
        @Override
        public FullHttpResponse handleRequest(FullHttpRequest request) {
            String content = "Another Route Handled!";
            return createHttpResponse(HttpResponseStatus.OK, content);
        }
    }

    private static FullHttpResponse createHttpResponse(HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(content, HttpConstants.DEFAULT_CHARSET));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        return response;
    }
}

