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
import org.apache.james.impl.jamesuser.LocalJamesUsersRepository;
import org.apache.james.impl.user.LocalUsersRepository;
import org.apache.james.impl.user.UserManagement;
import org.ops4j.peaberry.Export;
import org.ops4j.peaberry.Peaberry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class UsersLibraryActivator implements BundleActivator {
	@Inject
	private Provider<Log> logProvider;

	@Inject
	private Provider<HierarchicalConfiguration> configProvider;

	@Inject
	private Export<LocalJamesUsersRepository> jamesUsersRepos;

	@Inject
	private Export<LocalUsersRepository> localUsersRepos;

	@Inject
	Export<UserManagement> userMgmt;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		Injector inj = Guice.createInjector(Peaberry.osgiModule(context),
				new OSGIUserManagementModule(),
				new OSGILocalJamesUsersRepositoryModule(),
				new OSGILocalUsersRepositoryModule());

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
		jamesUsersRepos.unput();
		localUsersRepos.unput();
		userMgmt.unput();

		jamesUsersRepos = null;
		localUsersRepos = null;
		userMgmt = null;
	}

}
