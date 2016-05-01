package com.dhmin.test.netty.proxy.v2;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author DHMin
 */
public class ProxyMessageDecoder extends ByteToMessageDecoder {
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