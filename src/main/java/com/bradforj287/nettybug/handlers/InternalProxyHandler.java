package com.bradforj287.nettybug.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequestEncoder;

public class InternalProxyHandler extends SimpleChannelInboundHandler<HttpObject> {
    private Channel outboundChannel;

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

        final LocalAddress addr = new LocalAddress("internalServer");

        Bootstrap b = new Bootstrap();
        b.group(writeChanel.channel().eventLoop())
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    protected void initChannel(LocalChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpRequestEncoder());
                        p.addLast(backend);
                    }
                }).option(ChannelOption.AUTO_READ, false);

        return b.connect(addr);
    }
}