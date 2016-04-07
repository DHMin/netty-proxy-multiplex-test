package com.dhmin.test.netty.proxy.multiplexing.multiconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
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
public class MultiConnectBackendServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MultiConnectBackendServer.class);

	private static final int PORT = 11000;

	public static void main(String[] args) {
		new MultiConnectBackendServer().run();
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			 .channel(NioServerSocketChannel.class)
			 .handler(new LoggingHandler(LogLevel.DEBUG))
			 .childHandler(new ChannelInitializer<SocketChannel>() {
				 @Override
				 protected void initChannel(SocketChannel ch) throws Exception {
					 ch.pipeline().addLast(new MultiConnectBackendServerHandler());
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
}
