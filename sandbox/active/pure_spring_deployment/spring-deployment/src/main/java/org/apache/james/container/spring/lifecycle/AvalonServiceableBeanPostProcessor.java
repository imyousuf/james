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

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Mimic a ServiceManager and inject it to beans which implement Serviceable 
 * 
 *
 */
public class AvalonServiceableBeanPostProcessor extends AbstractLifeCycleBeanPostProcessor<Serviceable> implements ApplicationContextAware, ServiceManager {

	private Map<String, String> beanRoleMap = new HashMap<String, String>();
	private ApplicationContext context;
	
	@Override
	protected void executeLifecycleMethodBeforeInit(Serviceable bean,
			String beanname, String lifecyclename) throws Exception {
		bean.service(this);
	}

	@Override
	protected Class<Serviceable> getLifeCycleInterface() {
		return Serviceable.class;
	}


	/**
	 * Set mappings for role names to bean names
	 * 
	 * @param beanRoleMap
	 */
	public void setBeanRoleMap(Map<String,String> beanRoleMap) {
		this.beanRoleMap = beanRoleMap;
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.avalon.framework.service.ServiceManager#hasService(java.lang.String)
	 */
	public boolean hasService(String role) {
		String beanName = getBeanName(role);
		return context.containsBean(beanName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.avalon.framework.service.ServiceManager#lookup(java.lang.String)
	 */
	public Object lookup(String role) throws ServiceException {
		String beanName = getBeanName(role);
		if (context.containsBean(beanName)) {
			return context.getBean(beanName);
		}
		throw new ServiceException(role, "Unable to lookup Object for Role with name " + role);
	}

	/**
	 * Do nothing here
	 */
	public void release(Object arg0) {
		// Do nothing
	}
	
	private String getBeanName(String role) {
		if (beanRoleMap == null) {
			beanRoleMap = new HashMap<String, String>();
		}
		
		if (beanRoleMap.containsKey(role)) {
			role = beanRoleMap.get(role);
		}
		return role;
	}

}
