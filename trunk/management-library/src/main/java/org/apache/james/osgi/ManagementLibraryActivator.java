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
package org.apache.james.osgi;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.management.BayesianAnalyzerManagementMBean;
import org.apache.james.management.BayesianAnalyzerManagementService;
import org.apache.james.management.DomainListManagementMBean;
import org.apache.james.management.DomainListManagementService;
import org.apache.james.management.ProcessorManagementMBean;
import org.apache.james.management.ProcessorManagementService;
import org.apache.james.management.SpoolManagementMBean;
import org.apache.james.management.SpoolManagementService;
import org.ops4j.peaberry.Export;
import org.ops4j.peaberry.Peaberry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class ManagementLibraryActivator implements BundleActivator {

	@Inject
	private Export<BayesianAnalyzerManagementMBean> analyzerBean;

	@Inject
	private Export<BayesianAnalyzerManagementService> analyzerService;

	@Inject
	private Export<DomainListManagementMBean> domBean;

	@Inject
	private Export<DomainListManagementService> domService;

	@Inject
	private Export<ProcessorManagementMBean> procBean;

	@Inject
	private Export<ProcessorManagementService> procService;

	@Inject
	private Export<SpoolManagementMBean> spoolBean;

	@Inject
	private Export<SpoolManagementService> spoolService;

	@Inject
	private Provider<Log> logProvider;

	@Inject
	private Provider<HierarchicalConfiguration> configProvider;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		Injector inj = Guice.createInjector(Peaberry.osgiModule(context),
				new OSGIBayesianAnalyzerManagementModule(),
				new OSGIDomainListManagementModule(),
				new OSGIProcessorManagementModule(),
				new OSGISpoolManagementModule());

		/* Create bundle content */
		inj.injectMembers(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		analyzerBean.unput();
		analyzerService.unput();
		domService.unput();
		domBean.unput();
		procBean.unput();
		procService.unput();
		spoolBean.unput();
		spoolService.unput();

		analyzerBean = null;
		analyzerService = null;
		domService = null;
		domBean = null;
		procBean = null;
		procService = null;
		spoolBean = null;
		spoolService = null;

	}
}
