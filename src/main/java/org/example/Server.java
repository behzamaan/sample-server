package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    private final List<Integer> ports;

    public Server(Integer... ports) {
        this.ports = new ArrayList<>();
        this.ports.addAll(Arrays.asList(ports));
    }

    void boot() throws InterruptedException {
        // Configure the EventLoopGroups for handling incoming connections and processing I/O.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // For handling incoming connections.
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // For processing I/O.

        try {
            ServerBootstrap bootstrap = getServerBootstrap(bossGroup, workerGroup);

            extracted(bootstrap);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void extracted(ServerBootstrap bootstrap) throws InterruptedException {
            ChannelFuture future = null;
        for (Integer port : this.ports) {

            // Bind and start the server.
            future = bootstrap.bind(port).sync();

            System.out.println("Server started on port " + port);

        }
        assert future != null;
        // Wait until the server socket is closed.
        future.channel().closeFuture().sync();
    }

    private static ServerBootstrap getServerBootstrap(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()

                                .addLast(new HttpServerCodec()) // Codec for HTTP request and response.
                                .addLast(new HttpObjectAggregator(65536)) // Aggregator for handling full HTTP requests.
                                .addLast(new HttpServerHandler()); // Your custom HTTP request handler.

                    }
                });
        return bootstrap;
    }
}
