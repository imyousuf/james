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
package org.apache.james.container.spring.adaptor;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.util.Map;

/**
 * provides a Avalon-style service manager to all James components
 */
public class ServiceManagerBridge implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private Map beanBlockInfos;

	private class ServiceManagerInstance implements ServiceManager {

		private Map beanBlockInfo;
		
		public ServiceManagerInstance(Map beanBlockInfo) {
			this.beanBlockInfo=beanBlockInfo;
		}

		public Object lookup(String componentIdentifier)
				throws ServiceException {
			Object component = lookupByClassname(componentIdentifier);
			if (component == null)
				component = lookupByBeanname(componentIdentifier);

			if (component == null)
				throw new ServiceException("could not resolve dependency "
						+ componentIdentifier); // adhere to avalon service
												// manager contract
			return component;
		}

		private Object lookupByClassname(String className) {
			
			String beanName = getBeanName(className);
			
			if (beanName!=null) {
				System.out.println("Lookup configured "+beanName);
				return lookupByBeanname(beanName);
			}
			
			Class lookupClass = null;
			try {
				lookupClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				return null;
			}
			
			Map beansOfType = applicationContext.getBeansOfType(lookupClass);
			if (beansOfType.size() > 1) {
				System.err.println("not yet supported");
				Thread.dumpStack();
				System.exit(1);
				throw new RuntimeException("not yet supported");
			}
			if (beansOfType.size() == 0)
				return null; // try other method
			Object bean = beansOfType.values().iterator().next();
			return bean;
		}

		public boolean hasService(String componentIdentifier) {
			try {
				return null != lookup(componentIdentifier);
			} catch (ServiceException e) {
				return false;
			}
		}

		public void release(Object object) {
			throw new IllegalStateException("not yet implemented");
		}
		
		protected String getBeanName(String className) {
			String beanName = null;
			if (beanBlockInfo!=null) {
				
				beanName= (String) beanBlockInfo.get(className);
				System.out.println("We have a blockInfo! " +className+"  -> "+beanName);
			} 
			return beanName;
		}
	}
	
	private Object lookupByBeanname(String componentIdentifier) {
		return applicationContext.getBean(componentIdentifier);
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ServiceManager getInstance(String beanName) {
		return new ServiceManagerInstance(getBeanBlockInfo(beanName));
	}
	
	protected Map getBeanBlockInfo(String beanName) {
		Map blockInfo = null;
		if (beanBlockInfos!=null) {
			blockInfo= (Map) beanBlockInfos.get(beanName);
		} 
		return blockInfo;
	}

	public void setBeanBlockInfos(Map beanBlockInfos) {
		this.beanBlockInfos = beanBlockInfos;
	}
}
