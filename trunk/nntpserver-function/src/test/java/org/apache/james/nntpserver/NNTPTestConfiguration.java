/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.nntpserver;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.test.util.Util;

public class NNTPTestConfiguration extends DefaultConfiguration {

	private int m_nntpListenerPort;
	private boolean m_authRequired = false;

	public NNTPTestConfiguration(int m_nntpListenerPort) {
		super("nntpserver");
		this.m_nntpListenerPort = m_nntpListenerPort;
	}

	public void setUseAuthRequired() {
		m_authRequired = true;
	}

	public void init() {
		setAttribute("enabled", true);
		addChild(Util.getValuedConfiguration("port", "" + m_nntpListenerPort));
		DefaultConfiguration handlerConfig = new DefaultConfiguration("handler");
		handlerConfig.addChild(Util.getValuedConfiguration("helloName",
				"myMailServer"));
		handlerConfig.addChild(Util.getValuedConfiguration("connectiontimeout",
				"360000"));
		handlerConfig.addChild(Util.getValuedConfiguration("authRequired",
				m_authRequired + ""));

		addChild(handlerConfig);
	}

}
