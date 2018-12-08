package com.jbm.sample.pr;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.string.StringDecoder;

@Configuration
@ChannelHandler.Sharable
public class DecodeString extends StringDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		// it may be empty, then return null
		if (msg.isReadable()) {
			// ByteBuf may not expose array method for accessing the under
			// layer bytes
			byte[] bytes = new byte[msg.readableBytes()];
			int readerIndex = msg.readerIndex();
			msg.getBytes(readerIndex, bytes);
			out.add(bytes);
		}

	}
}
