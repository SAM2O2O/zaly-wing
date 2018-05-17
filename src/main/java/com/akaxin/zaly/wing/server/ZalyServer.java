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
package com.akaxin.zaly.wing.server;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.zaly.wing.command.Command;
import com.akaxin.zaly.wing.command.CommandResponse;
import com.akaxin.zaly.wing.executor.AbstracteExecutor;
import com.akaxin.zaly.wing.executor.SimpleExecutor;
import com.akaxin.zaly.wing.netty.codec.MessageDecoder;
import com.akaxin.zaly.wing.netty.codec.MessageEncoder;
import com.akaxin.zaly.wing.netty.handler.NettyServerHandler;
import com.akaxin.zaly.wing.thread.CustomThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-04-10 17:09:48
 */
public abstract class ZalyServer {
	private static final Logger logger = LoggerFactory.getLogger(ZalyServer.class);
	private AbstracteExecutor<Command, CommandResponse> executor;
	private ServerBootstrap bootstrap;
	private EventLoopGroup parentGroup;
	private EventLoopGroup childGroup;

	public ZalyServer() {
		try {
			bootstrap = new ServerBootstrap();
			int needThreadNum = Runtime.getRuntime().availableProcessors() + 1;
			// 处理服务端事件组
			parentGroup = new NioEventLoopGroup(needThreadNum * 5, new CustomThreadFactory("bim-boss-evenloopgroup"));
			// 处理客户端连接请求的事件组
			childGroup = new NioEventLoopGroup(needThreadNum, new CustomThreadFactory("bim-worker-evenloopgroup"));
			// 用户处理所有的channel
			bootstrap.group(parentGroup, childGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			/**
			 * 对应的是tcp/ip协议listen函数中的backlog参数，函数listen(int socketfd,int
			 * backlog)用来初始化服务端可连接队列. 服务端处理客户端连接请求是顺序处理的，所以同一时间只能处理一个客户端连接，多个客户端来的时候，
			 * 服务端将不能处理的客户端连接请求放在队列中等待处理，backlog参数指定了队列的大小
			 */
			bootstrap.option(ChannelOption.SO_BACKLOG, 2000);
			/**
			 * 允许监听的端口共存
			 */
			bootstrap.option(ChannelOption.SO_REUSEADDR, true);
			/**
			 * ChannelOption.SO_SNDBUF参数对应于套接字选项中的SO_SNDBUF，
			 * ChannelOption.SO_RCVBUF参数对应于套接字选项中的SO_RCVBUF这两个参数
			 * 用于操作接收缓冲区和发送缓冲区的大小，接收缓冲区用于保存网络协议站内收到的数据， 直到应用程序读取成功，发送缓冲区用于保存发送数据，直到发送成
			 */
			bootstrap.option(ChannelOption.SO_RCVBUF, 256 * 1024);
			bootstrap.option(ChannelOption.SO_SNDBUF, 256 * 1024);// 256 KB/字节
			/**
			 * 在4.x版本中，UnpooledByteBufAllocator是默认的allocator，尽管其存在某些限制。
			 * 现在PooledByteBufAllocator已经广泛使用一段时间，并且我们有了增强的缓冲区泄漏追踪机制，
			 * 所以是时候让PooledByteBufAllocator成为默认了。<br>
			 * 总结：Netty4使用对象池，重用缓冲区
			 */
			bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			/**
			 * 当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
			 */
			bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
			/**
			 * Nagle算法是将小的数据包组装为更大的帧然后进行发送，而不是输入一次发送一次, 因此在数据包不足的时候会等待其他数据的到了，组装成大的数据包进行发送，
			 * 虽然该方式有效提高网络的有效负载， 但是却造成了延时，
			 * 而该参数的作用就是禁止使用Nagle算法，使用于小数据即时传输，于TCP_NODELAY相对应的是TCP_CORK，
			 * 该选项是需要等到发送的数据量最大的时候，一次性发送
			 */
			bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
			bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			/**
			 * 接受缓存区，动态内存分配端的算法
			 */
			bootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
			// the ChannelHandler to use for serving the requests.
			bootstrap.handler(new LoggingHandler(LogLevel.DEBUG));
			// Set the ChannelHandler which is used to serve the request for the
			// Channel's
			bootstrap.childHandler(new NettyChannelInitializer());

			executor = new SimpleExecutor<Command, CommandResponse>();
			loadExecutor(executor);
		} catch (Exception e) {
			closeGracefully();
			logger.error("init netty server error.", e);
			System.exit(-10);
		}
	}

	public void start(String address, int port) throws Exception {
		try {
			if (bootstrap != null) {
				ChannelFuture channelFuture = bootstrap.bind(address, port).sync();
				channelFuture.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {

					@Override
					public void operationComplete(Future<? super Void> future) throws Exception {
						closeGracefully();
					}
				});
			}
		} catch (Exception e) {
			closeGracefully();
			throw new Exception("start netty server error", e);
		}
	}

	private class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
		@Override
		protected void initChannel(SocketChannel channel) throws Exception {
			// SSLEngine sslEngine =
			// NettySocketSslContext.getInstance().getServerContext().createSSLEngine();

			channel.pipeline().addLast(new MessageDecoder());
			channel.pipeline().addLast(new MessageEncoder());
			channel.pipeline().addLast("timeout", new IdleStateHandler(5, 5, 5, TimeUnit.MINUTES));

			// ch.pipeline().addLast(new SslHandler(sslEngine));

			channel.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler(5, TimeUnit.MINUTES));
			channel.pipeline().addLast("writeTimeoutHandler", new WriteTimeoutHandler(5, TimeUnit.MINUTES));
			channel.pipeline().addLast(new NettyServerHandler(executor));
		}

	}

	private void closeGracefully() {
		try {
			if (parentGroup != null) {
				// terminate all threads
				parentGroup.shutdownGracefully();
				// wait for all threads terminated
				parentGroup.terminationFuture().sync();
			}
			if (childGroup != null) {
				// terminate all threads
				childGroup.shutdownGracefully();
				// wait for all threads terminated
				childGroup.terminationFuture().sync();
			}
		} catch (Exception es) {
			logger.error("shutdown netty gracefully error.", es);
		}
	}

	public abstract void loadExecutor(AbstracteExecutor<Command, CommandResponse> executor);
}
