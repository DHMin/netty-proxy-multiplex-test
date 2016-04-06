package com.dhmin.test.netty.proxy.multiplex.multiconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author DHMin
 */
public class MultiConnectBackendServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(MultiConnectBackendServerHandler.class);

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.info("{} READ : {}", ctx.channel(), msg);
		ctx.channel().writeAndFlush(msg);
	}

}
