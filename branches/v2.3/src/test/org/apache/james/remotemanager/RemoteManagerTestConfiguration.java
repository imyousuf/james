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


package org.apache.james.remotemanager;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.test.util.Util;

public class RemoteManagerTestConfiguration extends DefaultConfiguration {

    private int m_remoteManagerListenerPort;
    private Integer m_connectionLimit = null;
    private String m_loginName = "testLogin";
    private String m_loginPassword = "testPassword";

    public RemoteManagerTestConfiguration(int smtpListenerPort) {
        super("smptserver");

        m_remoteManagerListenerPort = smtpListenerPort;
    }

    public void setConnectionLimit(int iConnectionLimit) {
        m_connectionLimit = new Integer(iConnectionLimit);
    }

    public String getLoginName() {
        return m_loginName;
    }

    public void setLoginName(String loginName) {
        m_loginName = loginName;
    }

    public String getLoginPassword() {
        return m_loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        m_loginPassword = loginPassword;
    }

    public void init() {

        setAttribute("enabled", true);

        addChild(Util.getValuedConfiguration("port", "" + m_remoteManagerListenerPort));
        if (m_connectionLimit != null) addChild(Util.getValuedConfiguration("connectionLimit", "" + m_connectionLimit.intValue()));

        DefaultConfiguration handlerConfig = new DefaultConfiguration("handler");
        handlerConfig.addChild(Util.getValuedConfiguration("helloName", "myMailServer"));
        handlerConfig.addChild(Util.getValuedConfiguration("connectiontimeout", "360000"));

        DefaultConfiguration adminAccounts = new DefaultConfiguration("administrator_accounts");
        
        DefaultConfiguration account = new DefaultConfiguration("account");
        
        account.setAttribute("login", m_loginName);
        account.setAttribute("password", m_loginPassword);

        adminAccounts.addChild(account);
        handlerConfig.addChild(adminAccounts);
        
        // handlerConfig.addChild(Util.getValuedConfiguration("prompt", ">"));

        handlerConfig.addChild(Util.createRemoteManagerHandlerChainConfiguration());
        addChild(handlerConfig);
    }

}
