/** 
 * Copyright 2018-2028 Akaxin Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.akaxin.zaly.wing.netty.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.zaly.proto.CoreProto;
import com.akaxin.zaly.wing.client.ZalyClient;
import com.akaxin.zaly.wing.command.RedisCommand;
import com.akaxin.zaly.wing.command.RedisCommandResponse;
import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-01-19 18:54:30
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RedisCommand> {
	private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

	private final ZalyClient zalyClient;

	public NettyClientHandler(ZalyClient zalyClient) {
		this.zalyClient = zalyClient;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RedisCommand redisCmd) {
		String version = redisCmd.getParameterByIndex(0);
		String action = redisCmd.getParameterByIndex(1);
		byte[] params = redisCmd.getBytesParamByIndex(2);

		try {
			CoreProto.TransportPackageData packageData = CoreProto.TransportPackageData.parseFrom(params);
			CoreProto.ErrorInfo errInfo = packageData.getErr();
			byte[] result = packageData.getData().toByteArray();
			this.zalyClient.handleResponse(new RedisCommandResponse(errInfo, result));
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause != null) {
			logger.error("netty client channel exeception happen.", cause);
		}
		ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	}

}
