package com.dhmin.test.netty.proxy.multiplexing.multiconnect;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.dhmin.test.netty.proxy.multiplexing.multiconnect.MultiConnectBackendServer;
import com.dhmin.test.netty.proxy.multiplexing.multiconnect.MultiConnectServer;
import com.dhmin.test.netty.proxy.multiplexing.multiconnect.MultiConnectServerHandler.ProxyHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author DHMin
 */
public class MultiConnectTest extends TestCase {

	private Thread serverThread;
	private Thread backendServerThread;

	@Before
	public void setUp() throws Exception {
		serverThread = new Thread(new MultiConnectServer());
		backendServerThread = new Thread(new MultiConnectBackendServer());

		serverThread.start();
		backendServerThread.start();
	}

	@After
	public void tearDown() throws Exception {
		backendServerThread.interrupt();
		backendServerThread.join();

		serverThread.interrupt();
		serverThread.join();
	}

	@org.junit.Test
	public void test() throws Exception {
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(eventLoopGroup)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .option(ChannelOption.SO_KEEPALIVE, true)
		 .handler(new ChannelInboundHandlerAdapter() {
			 @Override
			 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				 System.out.println(msg);
			 }
		 });

		ChannelFuture future = b.connect("127.0.0.1", 10000).sync();

		Channel channel = future.channel();

		ByteBuf buf = channel.alloc().buffer();
		buf.writeBytes("hi".getBytes());

		channel.writeAndFlush(buf).sync();
		
		TimeUnit.SECONDS.sleep(3);
	}
}
