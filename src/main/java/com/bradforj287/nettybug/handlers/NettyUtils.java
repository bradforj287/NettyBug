package com.bradforj287.nettybug.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class NettyUtils {

    public static ChannelFuture flushToChannel(final Channel writeTo,
                                               final Channel readFrom,
                                               final Object objToWrite) {

        return writeTo.writeAndFlush(objToWrite).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {

                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                    writeTo.close();
                    readFrom.close();
                } else {
                    readFrom.read();
                }
            }
        });
    }

}
