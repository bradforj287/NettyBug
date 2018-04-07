package com.bradforj287.nettybug.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class CustomHttpProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final String host;
    private final int port;
    private final SslContext sslContext;

    private Channel outboundChannel;

    public CustomHttpProxyHandler(String host, int port) {
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
                    ctx.read();
                } else {
                    ctx.close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        outboundChannel.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        NettyUtils.flushToChannel(outboundChannel, ctx.channel(), msg);
    }

    private ChannelFuture buildOutboundConnection(ChannelHandlerContext writeChanel) {
        SimpleChannelInboundHandler<ByteBuf> backend = new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                ctx.read();
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                writeChanel.close();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

                msg.retain();
                NettyUtils.flushToChannel(writeChanel.channel(), ctx.channel(), msg);
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
                        p.addLast(new HttpRequestEncoder());
                        p.addLast(backend);
                    }
                }).option(ChannelOption.AUTO_READ, false);

        return b.connect(host, port);
    }
}
