package com.dhmin.test.netty.proxy.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author DHMin
 */
public class SingleProxyConnectServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(SingleProxyConnectServer.class);

	private static final int PORT = 20000;

	public static void main(String[] args) {
		new SingleProxyConnectServer().run();
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		ProxyConnector proxyConnector = new ProxyConnector();

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			 .channel(NioServerSocketChannel.class)
			 .handler(new LoggingHandler(LogLevel.DEBUG))
			 .childHandler(new ChannelInitializer<SocketChannel>() {
				 @Override
				 protected void initChannel(SocketChannel ch) throws Exception {
					 ch.pipeline().addLast(new SingleProxyConnectServerHandler(proxyConnector));
				 }
			 })
			 .option(ChannelOption.SO_BACKLOG, 128)
			 .childOption(ChannelOption.SO_KEEPALIVE, true);

			ChannelFuture f = b.bind(PORT).sync();

			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			log.error("Interrupted: {}", e.getMessage(), e);
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static class SingleProxyConnectServerHandler extends ChannelInboundHandlerAdapter {
		private static final Logger log = LoggerFactory.getLogger(SingleProxyConnectServerHandler.class);

		private Channel proxyChannel;
		private ProxyConnector proxyConnector;

		public SingleProxyConnectServerHandler(ProxyConnector proxyConnector) {
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
			log.info("{} READ: {}, {}", ctx.channel(), proxyChannel, in);
			proxyChannel.writeAndFlush(new ProxyMessage(ctx.channel(), in));
		}
	}
}
