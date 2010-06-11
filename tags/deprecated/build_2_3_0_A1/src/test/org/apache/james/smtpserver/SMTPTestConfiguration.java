/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/


package org.apache.james.smtpserver;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.test.util.Util;

public class SMTPTestConfiguration extends DefaultConfiguration {

    private int m_smtpListenerPort;
    private int m_maxMessageSize = 0;
    private String m_authorizedAddresses = "127.0.0.0/8";
    private String m_authorizingMode = "false";
    private boolean m_verifyIdentity = false;
    private Integer m_connectionLimit = null;

    public SMTPTestConfiguration(int smtpListenerPort) {
        super("smptserver");

        m_smtpListenerPort = smtpListenerPort;
    }

    public void setMaxMessageSize(int kilobytes)
    {
        m_maxMessageSize = kilobytes;
    }
    
    public int getMaxMessageSize() {
        return m_maxMessageSize;
    }

    public String getAuthorizedAddresses() {
        return m_authorizedAddresses;
    }

    public void setAuthorizedAddresses(String authorizedAddresses) {
        m_authorizedAddresses = authorizedAddresses;
    }

    public void setAuthorizingNotRequired() {
        m_authorizingMode = "false";
        m_verifyIdentity = false; 
    }

    public void setAuthorizingRequired() {
        m_authorizingMode = "true";
        m_verifyIdentity = true; 
    }

    public void setAuthorizingAnnounce() {
        m_authorizingMode = "announce";
        m_verifyIdentity = true; 
    }

    public void setConnectionLimit(int iConnectionLimit) {
        m_connectionLimit = new Integer(iConnectionLimit);
    }

    public void init() {

        setAttribute("enabled", true);

        addChild(Util.getValuedConfiguration("port", "" + m_smtpListenerPort));
        if (m_connectionLimit != null) addChild(Util.getValuedConfiguration("connectionLimit", "" + m_connectionLimit.intValue()));
        
        DefaultConfiguration handlerConfig = new DefaultConfiguration("handler");
        handlerConfig.addChild(Util.getValuedConfiguration("helloName", "myMailServer"));
        handlerConfig.addChild(Util.getValuedConfiguration("connectiontimeout", "360000"));
        handlerConfig.addChild(Util.getValuedConfiguration("authorizedAddresses", m_authorizedAddresses));
        handlerConfig.addChild(Util.getValuedConfiguration("maxmessagesize", "" + m_maxMessageSize));
        handlerConfig.addChild(Util.getValuedConfiguration("authRequired", m_authorizingMode));
        if (m_verifyIdentity) handlerConfig.addChild(Util.getValuedConfiguration("verifyIdentity", "" + m_verifyIdentity));

        handlerConfig.addChild(Util.createRemoteManagerHandlerChainConfiguration());
        addChild(handlerConfig);
    }

}
