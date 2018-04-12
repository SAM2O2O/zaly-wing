package com.akaxin.zaly.wing.netty.codec;

import com.akaxin.zaly.wing.channel.ChannelSession;

import io.netty.util.AttributeKey;

public class ChannelKey {
	// 绑定在channel中的session
	public static final AttributeKey<ChannelSession> CHANNELSESSION = AttributeKey.valueOf("channelSession");
}
