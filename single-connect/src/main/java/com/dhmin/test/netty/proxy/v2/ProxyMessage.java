package com.dhmin.test.netty.proxy.v2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author DHMin
 */
@Getter
@AllArgsConstructor
public class ProxyMessage {
	private int channelHashCode;
	private int bodyLength;
	private ByteBuf byteBuf;

	public ProxyMessage(Channel channel, ByteBuf byteBuf) {
		this.channelHashCode = channel.hashCode();
		this.bodyLength = byteBuf.readableBytes();
		this.byteBuf = byteBuf;
	}
}