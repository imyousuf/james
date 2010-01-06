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
package org.apache.james.smtpserver;

import javax.annotation.Resource;

import org.apache.james.smtpserver.protocol.SMTPServerMBean;
import org.apache.james.socket.api.ProtocolServer;

public class SMTPServerMBeanImpl implements SMTPServerMBean{

    private ProtocolServer server;

    @Resource(name="org.apache.james.socket.api.ProtocolServer")
    public void setProtocolServer(ProtocolServer server) {
        this.server = server;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.SMTPServerMBean#getNetworkInterface()
     */
    public String getNetworkInterface() {
        return server.getNetworkInterface();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.SMTPServerMBean#getPort()
     */
    public int getPort() {
        return server.getPort();
    }

 
    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.SMTPServerMBean#getSocketType()
     */
    public String getSocketType() {
        return server.getSocketType();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.SMTPServerMBean#isEnabled()
     */
    public boolean isEnabled() {
        return server.isEnabled();
    }

}
