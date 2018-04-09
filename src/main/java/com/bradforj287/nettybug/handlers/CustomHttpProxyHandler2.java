package com.bradforj287.nettybug.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class CustomHttpProxyHandler2 extends SimpleChannelInboundHandler<HttpObject> {

    private final String host;
    private final int port;
    private final int lowWaterMark;
    private final int highWaterMark;
    private final SslContext sslContext;

    private Channel outboundChannel;

    public CustomHttpProxyHandler2(String host, int port, int lowWaterMark, int highWaterMark) {
        this.lowWaterMark = lowWaterMark;
        this.highWaterMark = highWaterMark;
        this.host = host;
        this.port = port;
        try {
            this.sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        buildOutboundConnection(ctx).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    outboundChannel = future.channel();
                    ctx.channel().config().setOption(ChannelOption.AUTO_READ, true);
                    ctx.read();
                } else {
                    ctx.close();
                }
            }
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean isWritable = ctx.channel().isWritable();
        outboundChannel.config().setOption(ChannelOption.AUTO_READ, isWritable);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        outboundChannel.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        outboundChannel.writeAndFlush(msg, outboundChannel.voidPromise());
    }

    private ChannelFuture buildOutboundConnection(ChannelHandlerContext writeChanel) {
        SimpleChannelInboundHandler<HttpObject> backend = new SimpleChannelInboundHandler<HttpObject>() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                ctx.read();
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                boolean isWritable = ctx.channel().isWritable();
                writeChanel.channel().config().setOption(ChannelOption.AUTO_READ, isWritable);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                writeChanel.close();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                if (msg instanceof HttpContent) {
                    ((HttpContent) msg).retain();
                }
                writeChanel.writeAndFlush(msg, writeChanel.voidPromise());
            }
        };

        Bootstrap b = new Bootstrap();
        b.group(writeChanel.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        if (port == 443) {
                            p.addLast(sslContext.newHandler(ch.alloc()));
                        }
                        p.addLast(new HttpClientCodec());
                        p.addLast(backend);
                    }
                }).option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowWaterMark, highWaterMark));

        return b.connect(host, port);
    }
}
