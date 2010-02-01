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


package org.apache.james.smtpserver.integration.fastfail;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.lifecycle.Configurable;

public class TarpitHandler extends org.apache.james.protocols.smtp.core.fastfail.TarpitHandler implements Configurable{

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration handlerConfiguration)
            throws ConfigurationException {
        int tarpitRcptCount = handlerConfiguration.getInt("tarpitRcptCount", 0);
        long tarpitSleepTime = handlerConfiguration.getLong("tarpitSleepTime", 5000);
        if (tarpitRcptCount == 0)
            throw new ConfigurationException(
                    "Please set the tarpitRcptCount bigger values as 0");

        if (tarpitSleepTime == 0)
            throw new ConfigurationException(
                    "Please set the tarpitSleepTimeto a bigger values as 0");

        setTarpitRcptCount(tarpitRcptCount);
        setTarpitSleepTime(tarpitSleepTime);
    }
}
