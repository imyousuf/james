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
package org.apache.james.impl.vut;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.ManageableVirtualUserTableException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.ManageableVirtualUserTable;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.Disposable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.InstanceFactory;

public class JamesVirtualUserTable implements ManageableVirtualUserTable, Configurable, LogEnabled{

	private HierarchicalConfiguration config;
	private Log log;
	private InstanceFactory instanceFactory;
	private VirtualUserTable vut;
	
	public Collection<String> getMappings(String user, String domain)
			throws ErrorMappingException {
		return vut.getMappings(user, domain);
	}

	public void configure(HierarchicalConfiguration config)
			throws ConfigurationException {
		this.config = config;
	}

	public void setLog(Log log) {
		this.log = log;
	}
	
	@Resource(name="instanceFactory")
	public void setInstanceFactory(InstanceFactory instanceFactory) {
		this.instanceFactory = instanceFactory;
	}

	@PostConstruct
	public void init() throws Exception {
		HierarchicalConfiguration conf = config.configurationAt("table");

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		String repName = conf.getString("[@name]", null);
		String repClass = conf.getString("[@class]");

		if (repName == null) {
			repName = repClass;
		}

		if (log.isDebugEnabled()) {
			log.debug("Starting " + repClass);
		}

		vut = (VirtualUserTable) instanceFactory.newInstance(loader
				.loadClass(repClass), log, conf);

		if (log.isInfoEnabled()) {
			StringBuffer logBuffer = new StringBuffer(64).append("Bean  ")
					.append(repName).append(" started.");
			log.info(logBuffer.toString());
		}

	}
	
	@PreDestroy
	public void destroy() {
		if (vut != null) {
			if (vut instanceof Disposable) {
				((Disposable) vut).dispose();
			}
		}
	}

	public boolean addAddressMapping(String user, String domain, String address)
			throws ManageableVirtualUserTableException {
		return getManagement().addAddressMapping(user, domain, address);
	}

	public boolean addAliasDomainMapping(String aliasDomain, String realDomain)
			throws ManageableVirtualUserTableException {
		return getManagement().addAliasDomainMapping(aliasDomain, realDomain);
	}

	public boolean addErrorMapping(String user, String domain, String error)
			throws ManageableVirtualUserTableException {
		return getManagement().addErrorMapping(user, domain, error);
	}

	public boolean addMapping(String user, String domain, String mapping)
			throws ManageableVirtualUserTableException {
		return getManagement().addMapping(user, domain, mapping);
	}

	public boolean addRegexMapping(String user, String domain, String regex)
			throws ManageableVirtualUserTableException {
		return getManagement().addRegexMapping(user, domain, regex);
	}

	public Map<String, Collection<String>> getAllMappings() {
		try {
			return getManagement().getAllMappings();
		} catch (ManageableVirtualUserTableException e) {
			return new HashMap<String, Collection<String>>();
		}
	}

	public Collection<String> getUserDomainMappings(String user, String domain)
			throws ManageableVirtualUserTableException {
		return getManagement().getUserDomainMappings(user, domain);
	}

	public boolean removeAddressMapping(String user, String domain,
			String address) throws ManageableVirtualUserTableException {
		return getManagement().removeAddressMapping(user, domain, address);
	}

	public boolean removeAliasDomainMapping(String aliasDomain,
			String realDomain) throws ManageableVirtualUserTableException {
		return getManagement().removeAliasDomainMapping(aliasDomain, realDomain);

	}

	public boolean removeErrorMapping(String user, String domain, String error)
			throws ManageableVirtualUserTableException {
		return getManagement().removeErrorMapping(user, domain, error);
	}

	public boolean removeMapping(String user, String domain, String mapping)
			throws ManageableVirtualUserTableException {
		return getManagement().removeMapping(user, domain, mapping);
	}

	public boolean removeRegexMapping(String user, String domain, String regex)
			throws ManageableVirtualUserTableException {
		return getManagement().removeRegexMapping(user, domain, regex);
	}
	
	private ManageableVirtualUserTable getManagement() throws ManageableVirtualUserTableException{
		if (vut instanceof ManageableVirtualUserTable) {
			return (ManageableVirtualUserTable) vut;
		} else {
			throw new ManageableVirtualUserTableException("VirtualUserTable implementation is not managable");
		}
	}
    
}
