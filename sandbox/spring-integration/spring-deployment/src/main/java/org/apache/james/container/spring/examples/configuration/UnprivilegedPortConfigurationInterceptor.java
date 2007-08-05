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
package org.apache.james.container.spring.examples.configuration;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.MutableConfiguration;
import org.apache.james.container.spring.configuration.ConfigurationInterceptor;

/**
 * re-maps all privileged ports into the 9000's range
 */
public class UnprivilegedPortConfigurationInterceptor implements ConfigurationInterceptor {
    
    public Configuration intercept(Configuration configuration) {
        interceptInternal(configuration);
        return configuration;
    }

    private void interceptInternal(Configuration configuration) {
        String name = configuration.getName();
        if ("port".equals(name) && configuration instanceof MutableConfiguration) {
            MutableConfiguration mutableConfiguration = (MutableConfiguration) configuration;
            int port = configuration.getValueAsInteger(0);
            if (port > 0 && port < 1024) {
                // map privileged port to unprivileged in the 9000's range
                port += 9000;
            }
            mutableConfiguration.setValue(port);
        }

        // go deep.
        Configuration[] children = configuration.getChildren();
        for (int i = 0; i < children.length; i++) {
            Configuration childConfiguration = children[i];
            interceptInternal(childConfiguration);
        }
    }
}
