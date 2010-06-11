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

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.smtpserver.core.CoreCmdHandlerLoader;
import org.apache.james.smtpserver.core.filter.fastfail.DNSRBLHandler;
import org.apache.james.smtpserver.core.filter.fastfail.MaxRcptHandler;
import org.apache.james.smtpserver.core.filter.fastfail.ResolvableEhloHeloHandler;
import org.apache.james.smtpserver.core.filter.fastfail.ReverseEqualsEhloHeloHandler;
import org.apache.james.smtpserver.core.filter.fastfail.ValidSenderDomainHandler;
import org.apache.james.test.mock.util.AttrValConfiguration;
import org.apache.james.test.util.Util;

public class SMTPTestConfiguration extends DefaultConfiguration {

    private int m_smtpListenerPort;
    private int m_maxMessageSizeKB = 0;
    private String m_authorizedAddresses = "127.0.0.0/8";
    private String m_authorizingMode = "false";
    private boolean m_verifyIdentity = false;
    private Integer m_connectionLimit = null;
    private Integer m_connectionBacklog = null;
    private boolean m_heloResolv = false;
    private boolean m_ehloResolv = false;
    private boolean m_senderDomainResolv = false;
    private boolean m_checkAuthNetworks = false;
    private boolean m_heloEhloEnforcement = true;
    private boolean m_reverseEqualsHelo = false;
    private boolean m_reverseEqualsEhlo = false;
    private int m_maxRcpt = 0;
    private boolean m_useRBL = false;
    private boolean m_addressBracketsEnforcement = true;
	private boolean m_startTLS = false;

    
    public SMTPTestConfiguration(int smtpListenerPort) {
        super("smptserver");

        m_smtpListenerPort = smtpListenerPort;
    }
    
    public void setCheckAuthNetworks(boolean checkAuth) {
        m_checkAuthNetworks = checkAuth; 
    }


    public void setMaxMessageSize(int kilobytes)
    {
        m_maxMessageSizeKB = kilobytes;
    }
    
    
    public int getMaxMessageSize() {
        return m_maxMessageSizeKB;
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
    
    public void setConnectionBacklog(int iConnectionBacklog) {
        m_connectionBacklog = new Integer(iConnectionBacklog);
    }
    
    public void setHeloResolv() {
        m_heloResolv = true; 
    }
    
    public void setEhloResolv() {
        m_ehloResolv = true; 
    }
    
    public void setReverseEqualsHelo() {
        m_reverseEqualsHelo = true; 
    }
    
    public void setReverseEqualsEhlo() {
        m_reverseEqualsEhlo = true; 
    }
    
    public void setSenderDomainResolv() {
        m_senderDomainResolv = true; 
    }
    
    public void setMaxRcpt(int maxRcpt) {
        m_maxRcpt = maxRcpt; 
    }
    
    public void setHeloEhloEnforcement(boolean heloEhloEnforcement) {
        m_heloEhloEnforcement = heloEhloEnforcement; 
    }
    
    public void useRBL(boolean useRBL) {
        m_useRBL = useRBL; 
    }
    
    public void setAddressBracketsEnforcement(boolean addressBracketsEnforcement) {
        this.m_addressBracketsEnforcement = addressBracketsEnforcement;
    }
    
    public void setStartTLS() {
    	m_startTLS  = true;
    }
    public void init() throws ConfigurationException {

        setAttribute("enabled", true);

        addChild(Util.getValuedConfiguration("port", "" + m_smtpListenerPort));
        if (m_connectionLimit != null) addChild(Util.getValuedConfiguration("connectionLimit", "" + m_connectionLimit.intValue()));
        if (m_connectionBacklog != null) addChild(Util.getValuedConfiguration("connectionBacklog", "" + m_connectionBacklog.intValue()));
        
        DefaultConfiguration handlerConfig = new DefaultConfiguration("handler");
        handlerConfig.addChild(Util.getValuedConfiguration("helloName", "myMailServer"));
        handlerConfig.addChild(Util.getValuedConfiguration("connectiontimeout", "360000"));
        handlerConfig.addChild(Util.getValuedConfiguration("authorizedAddresses", m_authorizedAddresses));
        handlerConfig.addChild(Util.getValuedConfiguration("maxmessagesize", "" + m_maxMessageSizeKB));
        handlerConfig.addChild(Util.getValuedConfiguration("authRequired", m_authorizingMode));
        handlerConfig.addChild(Util.getValuedConfiguration("heloEhloEnforcement", m_heloEhloEnforcement+""));
        handlerConfig.addChild(Util.getValuedConfiguration("addressBracketsEnforcement", m_addressBracketsEnforcement+""));
        
        DefaultConfiguration tlsConfig = new DefaultConfiguration("startTLS");
        tlsConfig.setAttribute("enable", m_startTLS);
        tlsConfig.addChild(new AttrValConfiguration("keystore","file://conf/test_keystore"));
        tlsConfig.addChild(new AttrValConfiguration("secret", "jamestest"));
        addChild(tlsConfig);
        
        if (m_verifyIdentity) handlerConfig.addChild(Util.getValuedConfiguration("verifyIdentity", "" + m_verifyIdentity));
 
        DefaultConfiguration config = new DefaultConfiguration("handlerchain");

        // add the rbl handler
        if (m_useRBL) {
            DefaultConfiguration handler = new DefaultConfiguration("handler");
            handler.setAttribute("class", DNSRBLHandler.class.getName());

            DefaultConfiguration blacklist = new DefaultConfiguration(
                    "blacklist");
            blacklist.setValue("bl.spamcop.net.");
            DefaultConfiguration rblServers = new DefaultConfiguration(
                    "rblservers");
            rblServers.addChild(blacklist);

            handler.addChild(rblServers);
            config.addChild(handler);

        }
        if (m_heloResolv || m_ehloResolv) {
            DefaultConfiguration d = createHandler(
                    ResolvableEhloHeloHandler.class.getName());
            d.addChild(Util.getValuedConfiguration("checkAuthNetworks",
                    m_checkAuthNetworks + ""));
            config.addChild(d);
        }
        if (m_reverseEqualsHelo || m_reverseEqualsEhlo) {
            DefaultConfiguration d = createHandler(
                    ReverseEqualsEhloHeloHandler.class.getName());
            d.addChild(Util.getValuedConfiguration("checkAuthNetworks",
                    m_checkAuthNetworks + ""));
            config.addChild(d);
        }
        if (m_senderDomainResolv) {
            DefaultConfiguration d = createHandler(
                    ValidSenderDomainHandler.class.getName());
            d.addChild(Util.getValuedConfiguration("checkAuthNetworks",
                    m_checkAuthNetworks + ""));
            config.addChild(d);
        }
        if (m_maxRcpt > 0) {
            DefaultConfiguration d = createHandler(MaxRcptHandler.class
                    .getName());
            d.addChild(Util.getValuedConfiguration("maxRcpt", m_maxRcpt + ""));
            config.addChild(d);
        }
       
        
        config.addChild(createHandler(CoreCmdHandlerLoader.class.getName()));
        handlerConfig.addChild(config);
        addChild(handlerConfig);
    }

    private DefaultConfiguration createHandler(String className) {
        DefaultConfiguration d = new DefaultConfiguration("handler");
       
        d.setAttribute("class", className);
        return d;
    }
    
}
