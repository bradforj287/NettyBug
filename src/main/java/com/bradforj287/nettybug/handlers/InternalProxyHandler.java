package com.bradforj287.nettybug.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;

public class InternalProxyHandler extends SimpleChannelInboundHandler<HttpObject> {
    private Channel outboundChannel;
    private final boolean autoRead;

    public InternalProxyHandler(boolean autoRead) {
        this.autoRead = autoRead;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        buildOutboundConnection(ctx).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    outboundChannel = future.channel();
                    ctx.channel().config().setOption(ChannelOption.AUTO_READ, autoRead);
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
        SimpleChannelInboundHandler<HttpObject> backend = new SimpleChannelInboundHandler<HttpObject>() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                ctx.read();
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
                NettyUtils.flushToChannel(writeChanel.channel(), ctx.channel(), msg);
            }
        };

        final LocalAddress addr = new LocalAddress("internalServer");

        Bootstrap b = new Bootstrap();
        b.group(writeChanel.channel().eventLoop())
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    protected void initChannel(LocalChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpClientCodec());
                        p.addLast(backend);
                    }
                }).option(ChannelOption.AUTO_READ, autoRead);

        return b.connect(addr);
    }
}