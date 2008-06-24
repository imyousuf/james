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

package org.apache.james.transport.mailets;

import org.apache.james.services.DNSServer;
import org.apache.james.transport.remotedeliverytester.RemoteDeliveryTestable;
import org.apache.james.transport.remotedeliverytester.Tester;

import javax.mail.Session;

import java.util.Properties;

/**
 * RemoteDelivery extension to publish test-aware interfaces
 */
public class StandardRemoteDeliveryTestable extends RemoteDelivery implements RemoteDeliveryTestable {
    
    public boolean logEnabled = true;
    private Tester tester;

    public void setRemoteDeliveryTester(Tester tester) {
        this.tester = tester;
    }

    protected Session obtainSession(Properties props) {
        if (tester != null) return tester.obtainSession(props);
        else return super.obtainSession(props);
    }

    public void log(String message, Throwable t) {
        if (logEnabled) super.log(message, t);
    }

    public void log(String message) {
        if (logEnabled) super.log(message);
    }
    
    public void setDNSServer(DNSServer dnsServer) {
        super.setDNSServer(dnsServer);
    }

}
