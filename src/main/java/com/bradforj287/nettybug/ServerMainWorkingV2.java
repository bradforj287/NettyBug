package com.bradforj287.nettybug;

import com.bradforj287.nettybug.handlers.CustomHttpProxyHandler2;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMainWorkingV2 {
    private static Logger logger = LoggerFactory.getLogger(ServerMainWorking.class);

    private final static int PORT = 9090;

    private static EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static void main(String[] args) throws Exception {
        logger.info("starting working server V2");
        logger.info(String.format("make http request to: http://localhost:%s", PORT));

        final int lowWatermark = 8*1024;
        final int highWatermark = 32*1024;
        // external server start
        ServerBootstrap b = new ServerBootstrap();
        b.group(eventLoopGroup)
               // .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast(new CustomHttpProxyHandler2("localhost", 5000, lowWatermark, highWatermark));
                    }
                }).childOption(ChannelOption.AUTO_READ, false)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowWatermark, highWatermark));

        Channel ch = b.bind(PORT).sync().channel();
        ch.closeFuture().sync();
    }
}
