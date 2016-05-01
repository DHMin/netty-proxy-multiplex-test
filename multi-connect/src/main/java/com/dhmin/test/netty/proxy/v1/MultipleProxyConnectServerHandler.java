package com.dhmin.test.netty.proxy.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author DHMin
 */
public class MultipleProxyConnectServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(MultipleProxyConnectServerHandler.class);

	private static final String BACKEND_ADDR = "127.0.0.1";
	private static final int BACKEND_PORT = 11000;

	private Channel proxyChannel;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("ACTIVE: {}", ctx.channel());

		final Bootstrap b = new Bootstrap();
		b.group(ctx.channel().eventLoop())
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .option(ChannelOption.SO_KEEPALIVE, true)
		 .handler(new ProxyHandler(ctx.channel()));

		b.connect(BACKEND_ADDR, BACKEND_PORT).addListener((ChannelFuture future) -> {
			if (future.isSuccess()) {
				proxyChannel = future.channel();
				log.info("{} connected: {}", ctx.channel(), proxyChannel);
			}
		});
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("INACTIVE: {}", ctx.channel());

		if (proxyChannel != null && proxyChannel.isActive()) {
			proxyChannel.close();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.info("{} READ: {}", ctx.channel(), msg);
		proxyChannel.writeAndFlush(msg);
	}

	class ProxyHandler extends ChannelInboundHandlerAdapter {
		private final Channel frontChannel;

		public ProxyHandler(Channel frontChannel) {
			this.frontChannel = frontChannel;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			frontChannel.writeAndFlush(msg);
		}

	}
}
