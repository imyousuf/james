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

package org.apache.james.services;


import javax.mail.internet.AddressException;

import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.MailUtil;

public class MockMailServer implements MailServer {

    
    private boolean virtualHosting;


    public synchronized String getId() {
        return MailUtil.newId();
    }

    public boolean isLocalServer(String serverName) {
        return "localhost".equals(serverName);
    }
    
    public void setVirtualHosting(boolean virtualHosting) {
        this.virtualHosting = virtualHosting;
    }

    public boolean supportVirtualHosting() {
        return virtualHosting;
    }

    public String getDefaultDomain() {
        return "localhost";
    }

    public String getHelloName() {
        return "localhost";
    }

    public MailAddress getPostmaster() {
        try {
            return new MailAddress("postmaster", "localhost");
        } catch (AddressException e) {
            return null;
        }
    }
}


