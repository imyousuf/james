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
package org.apache.james.container.spring.lifecycle;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.james.container.spring.AvalonConfigurationProvider;

/**
 * Inject Avalon Configuration to beans which implement Configurable 
 *
 */
public class AvalonConfigurableBeanPostProcessor extends AbstractLifeCycleBeanPostProcessor<Configurable>{

	private AvalonConfigurationProvider provider;

	@Override
	protected void executeLifecycleMethodBeforeInit(Configurable bean, String beanname,
			String lifecyclename) throws Exception {
		Configuration avalonConf = provider.getAvalonConfigurationForComponent(lifecyclename);
		bean.configure(avalonConf);
	}

	@Override
	protected Class<Configurable> getLifeCycleInterface() {
		return Configurable.class;
	}
	
	public void setConfigurationProvider(AvalonConfigurationProvider provider) {
		this.provider = provider;
	}

}
