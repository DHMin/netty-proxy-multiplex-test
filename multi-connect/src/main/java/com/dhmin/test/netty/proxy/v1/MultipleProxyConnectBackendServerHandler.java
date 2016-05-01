package com.dhmin.test.netty.proxy.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author DHMin
 */
public class MultipleProxyConnectBackendServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(MultipleProxyConnectBackendServerHandler.class);

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.info("{} READ : {}", ctx.channel(), msg);
		ctx.channel().writeAndFlush(msg);
	}

}
