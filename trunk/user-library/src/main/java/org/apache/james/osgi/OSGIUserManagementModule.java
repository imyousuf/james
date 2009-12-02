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

import org.apache.commons.logging.Log;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.api.user.management.UserManagementMBean;
import org.apache.james.impl.user.UserManagement;
import org.apache.james.impl.user.UserManagementModule;
import org.ops4j.peaberry.Peaberry;

public class OSGIUserManagementModule extends UserManagementModule{

	@Override
	protected void configure() {
		super.configure();
		
		// import osgi service
        bind(UsersStore.class).toProvider(Peaberry.service(UsersStore.class).single());
        bind(UsersRepository.class).toProvider(Peaberry.service(UsersRepository.class).single());
		bind(Log.class).toProvider(Peaberry.service(Log.class).single());

		
        // export it as osgi service
        bind(org.ops4j.peaberry.util.TypeLiterals.export(UserManagementMBean.class)).toProvider(Peaberry.service(UserManagement.class).export());
	}

}
