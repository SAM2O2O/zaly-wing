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

import com.akaxin.zaly.proto.CoreProto.ErrorInfo;
import com.google.gson.Gson;

public class RedisCommandResponse {

	private ErrorInfo errInfo;
	private byte[] params;

	public RedisCommandResponse(ErrorInfo errInfo, byte[] params) {
		this.errInfo = errInfo;
		this.params = params;
	}

	public boolean isSuccess() {
		if (this.errInfo != null) {
			return "success".equalsIgnoreCase(this.errInfo.getCode());
		}
		return false;
	}

	public ErrorInfo getErrInfo() {
		return errInfo;
	}

	public void setErrInfo(ErrorInfo errInfo) {
		this.errInfo = errInfo;
	}

	public byte[] getParams() {
		return params;
	}

	public void setParams(byte[] params) {
		this.params = params;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
