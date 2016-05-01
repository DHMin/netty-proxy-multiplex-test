package com.dhmin.test.netty.proxy.v2;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.util.ReferenceCountUtil;

/**
 * @author DHMin
 */
public class ProxyConnector {
	private static final Logger log = LoggerFactory.getLogger(SingleProxyConnectServer.class);

	private static final String BACKEND_ADDR = "127.0.0.1";
	private static final int BACKEND_PORT = 21000;

	private ConcurrentMap<Integer, Channel> channelHashCodeMap = new ConcurrentHashMap<>();

	private Channel proxyChannel;
	private volatile boolean init;

	public ProxyConnector() {
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(eventLoopGroup)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .option(ChannelOption.SO_KEEPALIVE, true)
		 .handler(new ChannelInitializer<Channel>() {

			 @Override
			 protected void initChannel(Channel ch) throws Exception {
				 ch.pipeline().addLast(new ProxyMessageCodec(),
									   new ProxyHandler());
			 }

		 });

		b.connect(BACKEND_ADDR, BACKEND_PORT).addListener((ChannelFuture future) -> {
			if (future.isSuccess()) {
				proxyChannel = future.channel();
				log.info("PROXY CONNECTED: {}", proxyChannel);
				init = true;
			}
		});
	}

	public Channel getProxyChannel(Channel frontChannel) {
		channelHashCodeMap.put(frontChannel.hashCode(), frontChannel);
		while (!init) {
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {}
		}
		return proxyChannel;
	}

	public void removeChannel(Channel frontChannel) {
		channelHashCodeMap.remove(frontChannel.hashCode());
	}

	class ProxyHandler extends ChannelDuplexHandler {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ProxyMessage pMsg = (ProxyMessage) msg;
			channelHashCodeMap.get(pMsg.getChannelHashCode()).writeAndFlush(pMsg.getByteBuf());
		}
	}

	class ProxyMessageCodec extends ByteToMessageCodec<ProxyMessage> {

		@Override
		protected void encode(ChannelHandlerContext ctx, ProxyMessage msg, ByteBuf out) throws Exception {
			out.writeInt(msg.getChannelHashCode());
			out.writeInt(msg.getBodyLength());
			out.writeBytes(msg.getByteBuf());
			ReferenceCountUtil.safeRelease(msg.getByteBuf());
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			in.markReaderIndex();

			try {
				int channelHashCode = in.readInt();
				int bodyLength = in.readInt();
				ByteBuf buf = ctx.alloc().buffer(bodyLength);
				in.readBytes(buf, bodyLength);
				out.add(new ProxyMessage(channelHashCode, bodyLength, buf));
			} catch (Exception e) {
				in.resetReaderIndex();
			}

		}

	}
}