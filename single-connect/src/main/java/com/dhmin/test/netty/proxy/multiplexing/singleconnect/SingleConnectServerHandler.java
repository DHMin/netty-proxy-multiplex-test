package com.dhmin.test.netty.proxy.multiplexing.singleconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dhmin.test.netty.proxy.multiplexing.singleconnect.ProxyConnector.ProxyMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author DHMin
 */
public class SingleConnectServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(SingleConnectServerHandler.class);

	private Channel proxyChannel;
	private ProxyConnector proxyConnector;

	public SingleConnectServerHandler(ProxyConnector proxyConnector) {
		this.proxyConnector = proxyConnector;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("ACTIVE: {}", ctx.channel());
		proxyChannel = proxyConnector.getProxyChannel(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("INACTIVE: {}", ctx.channel());
		proxyConnector.removeChannel(ctx.channel());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf in = (ByteBuf) msg;
		log.info("{} READ: {}", ctx.channel(), in);
		proxyChannel.writeAndFlush(new ProxyMessage(ctx.channel(), in));
	}
}
