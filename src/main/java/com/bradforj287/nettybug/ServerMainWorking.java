package com.bradforj287.nettybug;

import com.bradforj287.nettybug.handlers.CustomHttpProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMainWorking {
    private static Logger logger = LoggerFactory.getLogger(ServerMainWorking.class);

    private final static int PORT = 9090;

    private static EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static void main(String[] args) throws Exception {
        logger.info("starting working server 1");


        // external server start
        ServerBootstrap b = new ServerBootstrap();
        b.group(eventLoopGroup)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast(new CustomHttpProxyHandler("localhost", 5000));
                    }
                }).childOption(ChannelOption.AUTO_READ, false);

        Channel ch = b.bind(PORT).sync().channel();
        ch.closeFuture().sync();
    }
}
