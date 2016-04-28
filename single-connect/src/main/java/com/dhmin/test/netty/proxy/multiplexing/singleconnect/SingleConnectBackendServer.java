package com.dhmin.test.netty.proxy.multiplexing.singleconnect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

public class SingleConnectBackendServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(SingleConnectBackendServer.class);

	private static final int PYSICAL_PORT = 21000;
	private static final String LOCAL_PORT = "local_server";

	private static final LocalAddress LOCAL_ADDR = new LocalAddress(LOCAL_PORT);

	private static final AttributeKey<Integer> ID_KEY = AttributeKey.newInstance("ID_KEY");
	private static final AttributeKey<Channel> PYSICAL_CHANNEL = AttributeKey.newInstance("PYSICAL_CHANNEL");

	public static void main(String[] args) {
		new SingleConnectBackendServer().run();
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		EventLoopGroup localServerEventGroup = new LocalEventLoopGroup();

		try {
			ChannelFuture frontServerCloseFuture = runFrontServer(bossGroup, workerGroup);
			ChannelFuture localServerCloseFuture = runLocalServer(localServerEventGroup);

			frontServerCloseFuture.channel().closeFuture().sync();
			localServerCloseFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			log.error("Interrupted: {}", e.getMessage(), e);
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	private ChannelFuture runLocalServer(EventLoopGroup eventGroup)
			throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
		b.group(eventGroup)
		 .channel(LocalServerChannel.class)
		 .handler(new LoggingHandler(LogLevel.DEBUG))
		 .childHandler(new ChannelInitializer<LocalChannel>() {
			 @Override
			 protected void initChannel(LocalChannel ch) throws Exception {
				 ch.pipeline().addLast(new LocalServerHandelr());
			 }
		 });

		return b.bind(LOCAL_ADDR).sync();
	}

	private ChannelFuture runFrontServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup)
			throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		 .channel(NioServerSocketChannel.class)
		 .handler(new LoggingHandler(LogLevel.DEBUG))
		 .childHandler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 protected void initChannel(SocketChannel ch) throws Exception {
				 ch.pipeline().addLast(new PysicalServerHandler());
			 }
		 })
		 .option(ChannelOption.SO_BACKLOG, 128)
		 .childOption(ChannelOption.SO_KEEPALIVE, true);

		return b.bind(PYSICAL_PORT).sync();
	}

	public static class PysicalServerHandler extends ChannelInboundHandlerAdapter {
		private static final ConcurrentMap<Integer, Channel> LOCAL_CHANNEL_MAP = new ConcurrentHashMap<>();

		private EventLoopGroup eventGroup = new NioEventLoopGroup();
		private Bootstrap cb = new Bootstrap();

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			cb.group(eventGroup)
			  .channel(LocalChannel.class)
			  .handler(new ChannelInitializer<LocalChannel>() {
				  @Override
				  public void initChannel(LocalChannel ch) throws Exception {
					  ch.pipeline().addLast(new LocalClientHandler());
				  }
			  });
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			eventGroup.shutdownGracefully();
			super.channelInactive(ctx);
			LOCAL_CHANNEL_MAP.forEach((id, channel) -> {
				channel.close();
			});
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf in = (ByteBuf) msg;
			try {
				int id = in.readInt();
				Channel localChannel = LOCAL_CHANNEL_MAP.get(id);
				if (localChannel != null) {
					localChannel = createLocalChannel();
					localChannel.attr(ID_KEY).set(id);
					localChannel.attr(PYSICAL_CHANNEL).set(ctx.channel());
					Channel prevChannel = LOCAL_CHANNEL_MAP.put(id, localChannel);
					if (prevChannel != null) {
						prevChannel.close();
					}
				}

				ByteBuf buf = localChannel.alloc().buffer();
				buf.writeBytes(in);
				localChannel.writeAndFlush(buf);
			} finally {
				ReferenceCountUtil.safeRelease(in);
			}
		}

		private Channel createLocalChannel() throws InterruptedException {
			return cb.connect(LOCAL_ADDR).sync().channel();
		}
	}

	public static class LocalServerHandelr extends ChannelInboundHandlerAdapter {
		private static final Logger log = LoggerFactory.getLogger(LocalServerHandelr.class);

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			log.info("{} READ : {}", ctx.channel(), msg);
			ctx.channel().writeAndFlush(msg);
		}
	}

	public static class LocalClientHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf in = (ByteBuf) msg;
			try {
				Channel channel = ctx.channel();
				Integer id = channel.attr(ID_KEY).get();

				Channel pChannel = channel.attr(PYSICAL_CHANNEL).get();
				ByteBuf out = pChannel.alloc().buffer();
				out.writeInt(id);
				out.writeBytes(in);
				pChannel.writeAndFlush(out);
			} finally {
				ReferenceCountUtil.safeRelease(in);
			}
		}

	}
}
