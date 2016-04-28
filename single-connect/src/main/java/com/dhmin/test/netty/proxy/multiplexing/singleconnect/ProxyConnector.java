package com.dhmin.test.netty.proxy.multiplexing.singleconnect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import lombok.Value;

public class ProxyConnector {
	private static final Logger log = LoggerFactory.getLogger(SingleConnectServer.class);

	private static final String BACKEND_ADDR = "127.0.0.1";
	private static final int BACKEND_PORT = 21000;

	private ConcurrentMap<Integer, Channel> channelHashCodeMap = new ConcurrentHashMap<>();

	private Channel proxyChannel;

	public ProxyConnector() {
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(eventLoopGroup)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .option(ChannelOption.SO_KEEPALIVE, true)
		 .handler(new ProxyHandler());

		b.connect(BACKEND_ADDR, BACKEND_PORT).addListener((ChannelFuture future) -> {
			if (future.isSuccess()) {
				proxyChannel = future.channel();
				log.info("PROXY CONNECTED: {}", proxyChannel);
			}
		});
	}

	public Channel getProxyChannel(Channel frontChannel) {
		channelHashCodeMap.put(frontChannel.hashCode(), frontChannel);
		return proxyChannel;
	}

	public void removeChannel(Channel frontChannel) {
		channelHashCodeMap.remove(frontChannel.hashCode());
	}

	class ProxyHandler extends ChannelDuplexHandler {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf in = (ByteBuf) msg;
			int channelHashCode = in.readInt();
			channelHashCodeMap.get(channelHashCode).writeAndFlush(in);
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (msg instanceof ProxyMessage) {
				ProxyMessage pMsg = (ProxyMessage) msg;
				ByteBuf out = ctx.alloc().buffer();
				out.writeInt(pMsg.channel.hashCode());
				out.writeBytes(pMsg.byteBuf);
				super.write(ctx, out, promise);
				ReferenceCountUtil.safeRelease(pMsg.byteBuf);
			} else {
				super.write(ctx, msg, promise);
			}

		}
	}

	@Value
	public static class ProxyMessage {
		private Channel channel;
		private ByteBuf byteBuf;
	}
}