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
package com.akaxin.zaly.wing.client;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.zaly.wing.command.RedisCommand;
import com.akaxin.zaly.wing.command.RedisCommandResponse;
import com.akaxin.zaly.wing.netty.codec.MessageDecoder;
import com.akaxin.zaly.wing.netty.codec.MessageEncoder;
import com.akaxin.zaly.wing.netty.handler.ZalysClient2Handler;
import com.akaxin.zaly.wing.netty.ssl.NettySocketSslContext2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;

/**
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-01-19 18:14:16
 */
public class ZalySSLClient {
	private static final Logger logger = LoggerFactory.getLogger(ZalySSLClient.class);
	private volatile ChannelPromise channelPromise;
	private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
	private final Bootstrap clientBoot = new Bootstrap();
	private Promise<RedisCommandResponse> responsePromise;
	private static final Exception CONNECT_EXCEPTION = new Exception("client connect to server error");
	private ZalysClient2Handler zalyClientHandler;
	private String inetHost;
	private int inetPort;

	public ZalySSLClient() {
		try {
			clientBoot.option(ChannelOption.TCP_NODELAY, true);
			clientBoot.group(eventLoopGroup);
			clientBoot.channel(NioSocketChannel.class);
			clientBoot.option(ChannelOption.TCP_NODELAY, true);
			clientBoot.handler(new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(Channel channel) throws Exception {
					SslContext sslContext = NettySocketSslContext2.getSSLContext();
					SSLEngine sslEngine = sslContext.newEngine(channel.alloc(), inetHost, inetPort);
					channel.pipeline().addLast(new SslHandler(sslEngine));

					channel.pipeline().addLast(new MessageEncoder());
					channel.pipeline().addLast(new MessageDecoder());
					channel.pipeline().addLast(new IdleStateHandler(5, 5, 5, TimeUnit.MINUTES));
					channel.pipeline().addLast(new WriteTimeoutHandler(5, TimeUnit.MINUTES));
					channel.pipeline().addLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES));

					zalyClientHandler = new ZalysClient2Handler(ZalySSLClient.this);
					channel.pipeline().addLast(zalyClientHandler);
				}

			});
		} catch (Exception e) {
			shutDownGracefully();
			logger.error("init netty client error.", e);
		}
	}

	public Future<Void> connect(String address, int port) {
		this.inetHost = address;
		final Future<Void> connectionFuture;
		synchronized (clientBoot) {
			if (this.channelPromise == null) {
				try {
					final ChannelFuture connectFuture = this.clientBoot.connect(address, port).sync();
					this.channelPromise = connectFuture.channel().newPromise();

				} catch (Exception e) {
					logger.error("connect to akaxin platform error.", e);
				}

			}
			connectionFuture = this.channelPromise;
		}
		return connectionFuture;
	}

	public void shutDownGracefully() {
		try {
			if (eventLoopGroup != null) {
				eventLoopGroup.shutdownGracefully();
				eventLoopGroup.terminationFuture().sync();
			}
		} catch (InterruptedException e) {
			logger.error("shutdown netty client error.", e);
		}
	}

	public Future<RedisCommandResponse> sendRedisCommand(final RedisCommand redisCommand) {
		final Future<RedisCommandResponse> responseFuture;
		if (channelPromise != null) {
			final ChannelPromise readyPromise = this.channelPromise;

			final DefaultPromise<RedisCommandResponse> responsePromise = new DefaultPromise<RedisCommandResponse>(
					readyPromise.channel().eventLoop());
			// 提交一个事件
			readyPromise.channel().eventLoop().submit(new Runnable() {
				@Override
				public void run() {
					// 将这个结果赋值给responsePromise
					ZalySSLClient.this.responsePromise = responsePromise;
				}
			});

			readyPromise.channel().writeAndFlush(redisCommand).addListener(new GenericFutureListener<ChannelFuture>() {
				@Override
				public void operationComplete(final ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						// 如果失败了，直接将promise返回
						responsePromise.tryFailure(future.cause());
						logger.error("send push message error: action={},cause={}", redisCommand.getByteSize(),
								future.cause());
					} else {
						// logger.info("write data to platform success");
					}
				}
			});
			responseFuture = responsePromise;
		} else {
			logger.error("send push error because client is not connected: {}", redisCommand.toString());
			responseFuture = new FailedFuture<RedisCommandResponse>(GlobalEventExecutor.INSTANCE, CONNECT_EXCEPTION);
		}
		return responseFuture;
	}

	// 提交一次tcp请求结果
	public void handleResponse(final RedisCommandResponse response) {
		try {
			this.responsePromise.setSuccess(response);
		} catch (Exception e) {
			logger.error("handlePushNotificationResponse error!", e);
		}
	}

	public void disconnect() {
		synchronized (this.clientBoot) {
			this.channelPromise = null;
			final Future<Void> channelCloseFuture;
			if (this.channelPromise != null) {
				channelCloseFuture = this.channelPromise.channel().close();
			} else {
				channelCloseFuture = new SucceededFuture<Void>(GlobalEventExecutor.INSTANCE, null);
			}
			channelCloseFuture.addListener(new GenericFutureListener<Future<Void>>() {
				@Override
				public void operationComplete(final Future<Void> future) throws Exception {
					ZalySSLClient.this.clientBoot.config().group().shutdownGracefully();
				}
			});
		}
	}
}