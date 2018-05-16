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

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.zaly.proto.CoreProto;
import com.akaxin.zaly.wing.channel.ChannelManager;
import com.akaxin.zaly.wing.channel.ChannelSession;
import com.akaxin.zaly.wing.command.Command;
import com.akaxin.zaly.wing.command.CommandResponse;
import com.akaxin.zaly.wing.command.RedisCommand;
import com.akaxin.zaly.wing.executor.AbstracteExecutor;
import com.akaxin.zaly.wing.logs.LogUtils;
import com.akaxin.zaly.wing.netty.codec.ChannelKey;
import com.akaxin.zaly.wing.utils.StringHelper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Netty处理TCP链接中接受客户端传入的消息
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2017.11.07 16:56:36
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<RedisCommand> {
	private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	private AbstracteExecutor<Command, CommandResponse> executor;

	public NettyServerHandler(AbstracteExecutor<Command, CommandResponse> executor) {
		this.executor = executor;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientIp = socketAddress.getAddress().getHostAddress();
		ctx.channel().attr(ChannelKey.CHANNELSESSION).set(new ChannelSession(ctx.channel()));
		logger.debug("client={} connect to Netty Server...", clientIp);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientIp = socketAddress.getAddress().getHostAddress();
		logger.debug("client={} close connection... ChannelSize={}", clientIp, ChannelManager.getChannelSessionSize());
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RedisCommand redisCmd) throws Exception {
		InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientIp = socketAddress.getAddress().getHostAddress();
		ChannelSession channelSession = ctx.channel().attr(ChannelKey.CHANNELSESSION).get();

		// Channel不可用情况下，关闭连接事件
		// disconnect tcp connection as channel is unavailable
		if (channelSession.getChannel() == null || !channelSession.getChannel().isActive()) {
			ctx.disconnect();// 断开连接事件(与对方的连接断开)
			logger.warn("close client={} as its channel is not active ", clientIp);
		}

		String version = redisCmd.getParameterByIndex(0);
		String action = redisCmd.getParameterByIndex(1);
		byte[] params = redisCmd.getBytesParamByIndex(2);
		CoreProto.TransportPackageData packageData = CoreProto.TransportPackageData.parseFrom(params);

		Command command = new Command();
		command.setClientIp(clientIp);
		command.setAction(action);
		command.setHeader(packageData.getHeaderMap());
		command.setParams(packageData.getData().toByteArray());
		command.setChannelSession(channelSession);
		command.setStartTime(System.currentTimeMillis());

		logger.debug("client={} -> site version={} action={} params-length={}", clientIp, version, action,
				params.length);

		CommandResponse response = this.executor.execute(command.getRety(), command);
		// 输出API请求结果
		LogUtils.requestResultLog(logger, command, response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientIp = socketAddress.getAddress().getHostAddress();
		ctx.close();
		logger.error(StringHelper.format("client{} channel exeception happen.", clientIp), cause);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	}

}