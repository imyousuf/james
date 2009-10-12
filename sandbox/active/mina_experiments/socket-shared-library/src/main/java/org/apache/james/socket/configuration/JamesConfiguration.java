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


package org.apache.james.socket.configuration;

import java.io.ByteArrayInputStream;
import org.apache.avalon.framework.configuration.ConfigurationUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;

/**
 * Bridges commons configuration to Avalon.
 */
public class JamesConfiguration extends XMLConfiguration {

	private static final long serialVersionUID = 6920719067623856243L;

	public JamesConfiguration(org.apache.avalon.framework.configuration.Configuration avalonConfig) throws ConfigurationException {
		String config = ConfigurationUtil.toString(avalonConfig);
		load(new ByteArrayInputStream(config.getBytes()));
		
		setExpressionEngine(new XPathExpressionEngine());
	}
}
