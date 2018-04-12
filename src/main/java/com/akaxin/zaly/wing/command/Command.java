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
package com.akaxin.zaly.wing.command;

import java.util.HashMap;
import java.util.Map;

import com.akaxin.zaly.wing.channel.ChannelSession;
import com.akaxin.zaly.wing.constant.CommandConst;

import io.netty.channel.ChannelHandlerContext;

/**
 * 处理完成后的消息载体
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2017.09.30
 */
public class Command {
	private String clientIp;
	private String version;//
	private String rety; // request type
	private String service;// action = rety+service+method
	private String method;
	private String uri;
	private Map<Integer, String> header;
	private byte[] params;
	private Map<String, Object> fields = new HashMap<String, Object>();

	public void setAction(String splitStrs) {
		String[] splitStr = splitStrs.split("\\.");
		this.rety = splitStr[0];
		this.service = splitStr[1];
		if (splitStr.length == 3) {
			this.method = splitStr[2];
		}
	}

	public String getAction() {
		return this.rety + "." + this.service + "." + this.method;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getRety() {
		return rety;
	}

	public void setRety(String rety) {
		this.rety = rety;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Map<Integer, String> getHeader() {
		return header;
	}

	public void setHeader(Map<Integer, String> header) {
		this.header = header;
	}

	public byte[] getParams() {
		return params;
	}

	public Command setParams(byte[] params) {
		this.params = params;
		return this;
	}

	public Map<String, Object> getFields() {
		return fields;
	}

	@SuppressWarnings("unchecked")
	public <T> T getField(String k, Class<T> t) {
		Object obj = fields.get(k);
		return obj == null ? null : (T) obj;
	}

	public Command setFields(Map<String, Object> map) {
		this.fields.putAll(map);
		return this;
	}

	public Command setField(String k, Object v) {
		this.fields.put(k, v);
		return this;
	}

	public Command setStartTime(long time) {
		this.fields.put(CommandConst.START_TIME, time);
		return this;
	}

	public long getStartTime() {
		Long time = this.getField(CommandConst.START_TIME, Long.class);
		return time == null ? 0 : time;
	}

	public Command setEndTime(long time) {
		this.fields.put(CommandConst.END_TIME, time);
		return this;
	}

	public long getEndTime() {
		Long time = this.getField(CommandConst.END_TIME, Long.class);
		return time == null ? 0 : time;
	}

	public Command setChannelSession(ChannelSession channelSession) {
		this.fields.put(CommandConst.CHAHHEL_SESSION, channelSession);
		return this;
	}

	public ChannelSession getChannelSession() {
		return this.getField(CommandConst.CHAHHEL_SESSION, ChannelSession.class);
	}

	public Command setChannelContext(ChannelHandlerContext channelContext) {
		this.fields.put(CommandConst.CHANNEL_CONTEXT, channelContext);
		return this;
	}

	public ChannelHandlerContext getChannelContext() {
		return this.getField(CommandConst.CHANNEL_CONTEXT, ChannelHandlerContext.class);
	}

	public String toString() {
		return "version=" + this.version + ", rety=" + this.rety + ", service=" + this.service + ", method="
				+ this.method + ", uri=" + this.uri + ", header={}" + this.header;
	}

}
