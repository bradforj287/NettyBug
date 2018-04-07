package com.bradforj287.nettybug;

import com.bradforj287.nettybug.handlers.CustomHttpProxyHandler;
import com.bradforj287.nettybug.handlers.InternalProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerMainBroken {
    private static Logger logger = LoggerFactory.getLogger(ServerMainBroken.class);

    private final static int PORT = 9090;

    private static EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static void main(String[] args) throws Exception {
        logger.info("starting broken server");

        // internal server start
        final LocalAddress addr = new LocalAddress("internalServer");

        ServerBootstrap sb = new ServerBootstrap();
        sb.group(eventLoopGroup)
                .channel(LocalServerChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    protected void initChannel(LocalChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast(new CustomHttpProxyHandler("localhost", 5000));
                    }
                })
                .childOption(ChannelOption.AUTO_READ, false);

        // Start the server.
        sb.bind(addr).sync();

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
                        p.addLast(new InternalProxyHandler());
                    }
                }).childOption(ChannelOption.AUTO_READ, false);

        Channel ch = b.bind(PORT).sync().channel();
        ch.closeFuture().sync();
    }
}