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

import com.google.gson.Gson;

public class RedisCommandResponse {

	private final RedisCommand redisCommand;
	private final String errCode;
	private final String errInfo;

	public RedisCommandResponse(RedisCommand redisCommand, String errCode, String errInfo) {
		this.redisCommand = redisCommand;
		this.errCode = errCode;
		this.errInfo = errInfo;
	}

	public RedisCommand getRedisCommand() {
		return this.redisCommand;
	}

	public boolean isSuccess() {
		return "success".equalsIgnoreCase(this.errCode);
	}

	public String getErrInfo() {
		return this.errInfo;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
