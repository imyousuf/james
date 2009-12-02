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

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.management.BayesianAnalyzerManagementMBean;
import org.apache.james.management.BayesianAnalyzerManagementService;
import org.apache.james.management.impl.BayesianAnalyzerManagementModule;
import org.apache.james.services.FileSystem;
import org.ops4j.peaberry.Peaberry;

import com.google.inject.name.Names;

public class OSGIBayesianAnalyzerManagementModule extends BayesianAnalyzerManagementModule{

	@Override
	protected void configure() {
		super.configure();

		// import osgi service
		bind(FileSystem.class).toProvider(Peaberry.service(FileSystem.class).single());
		bind(DataSourceSelector.class).toProvider(Peaberry.service(DataSourceSelector.class).single());

		bind(Log.class).toProvider(Peaberry.service(Log.class).single());
		bind(HierarchicalConfiguration.class).toProvider(Peaberry.service(HierarchicalConfiguration.class).single());
		
		// bind annotations to get the right logger and config
		bind(String.class).annotatedWith(Names.named("LoggerName")).toInstance("bayesiananalyzermanagement");
		bind(String.class).annotatedWith(Names.named("ConfigurationName")).toInstance("bayesiananalyzermanagement");
		
		// export it as osgi service
		bind(org.ops4j.peaberry.util.TypeLiterals.export(BayesianAnalyzerManagementMBean.class)).toProvider(
				Peaberry.service(BayesianAnalyzerManagementMBean.class).export());
		bind(org.ops4j.peaberry.util.TypeLiterals.export(BayesianAnalyzerManagementService.class)).toProvider(
				Peaberry.service(BayesianAnalyzerManagementService.class).export());
	}
}
