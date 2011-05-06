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
package org.apache.james.lmtpserver.netty;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.protocols.lib.ConfigurableProtocolHandlerchain;
import org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.smtpserver.netty.SMTPChannelUpstreamHandler;
import org.apache.james.smtpserver.netty.SMTPResponseEncoder;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public class LMTPServer extends AbstractConfigurableAsyncServer implements LMTPServerMBean {

    /**
     * The maximum message size allowed by this SMTP server. The default value,
     * 0, means no limit.
     */
    private long maxMessageSize = 0;
    private ConfigurableProtocolHandlerchain handlerChain;
    private LMTPConfiguration lmtpConfig = new LMTPConfiguration();
    private String lmtpGreeting;
    private HierarchicalConfiguration config;

    @Resource(name = "lmtphandlerchain")
    public void setProtocolHandlerChain(ConfigurableProtocolHandlerchain handlerChain) {
        this.handlerChain = handlerChain;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer#
     * getDefaultPort()
     */
    public int getDefaultPort() {
        return 24;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#getServiceType()
     */
    public String getServiceType() {
        return "LMTP Service";
    }

    @Override
    protected void preInit() throws Exception {
        super.preInit();
        HierarchicalConfiguration hconfig = config.configurationAt("handlerchain");
        hconfig.addProperty("[@jmxName]", jmxName);
        hconfig.addProperty("[@jmxHandlersPackage]", "org.apache.james.lmtpserver.jmx.JMXHandlersLoader");
        hconfig.addProperty("[@coreHandlersPackage]", "org.apache.james.lmtpserver.CoreCmdHandlerLoader");

        handlerChain.init(hconfig);
    }

    public void doConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        this.config = configuration;
   
        if (isEnabled()) {

            // get the message size limit from the conf file and multiply
            // by 1024, to put it in bytes
            maxMessageSize = configuration.getLong("maxmessagesize", maxMessageSize) * 1024;
            if (maxMessageSize > 0) {
                getLogger().info("The maximum allowed message size is " + maxMessageSize + " bytes.");
            } else {
                getLogger().info("No maximum message size is enforced for this server.");
            }

            // get the lmtpGreeting
            lmtpGreeting = configuration.getString("lmtpGreeting", null);

        }
    }

    /**
     * A class to provide SMTP handler configuration to the handlers
     */
    public class LMTPConfiguration implements SMTPConfiguration {

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getHelloName()
         */
        public String getHelloName() {
            return LMTPServer.this.getHelloName();
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getResetLength()
         */
        public int getResetLength() {
            return -1;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return LMTPServer.this.maxMessageSize;
        }

        /**
         * Relaying not allowed with LMTP
         */
        public boolean isRelayingAllowed(String remoteIP) {
            return false;
        }

        /**
         * No enforcement
         */
        public boolean useHeloEhloEnforcement() {
            return false;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return LMTPServer.this.lmtpGreeting;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return true;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isAuthRequired(java.lang.String)
         */
        public boolean isAuthRequired(String remoteIP) {
            return true;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isStartTLSSupported()
         */
        public boolean isStartTLSSupported() {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.smtpserver.SMTPServerMBean#getMaximalMessageSize()
     */
    public long getMaximalMessageSize() {
        return lmtpConfig.getMaxMessageSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.protocols.smtp.SMTPServerMBean#getNetworkInterface()
     */
    public String getNetworkInterface() {
        return "unknown";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer#
     * getDefaultJMXName()
     */
    protected String getDefaultJMXName() {
        return "lmtpserver";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.smtpserver.netty.SMTPServerMBean#setMaximalMessageSize
     * (long)
     */
    public void setMaximalMessageSize(long maxSize) {
        maxMessageSize = maxSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.lmtpserver.netty.LMTPServerMBean#getHeloName()
     */
    public String getHeloName() {
        return lmtpConfig.getHelloName();
    }

    @Override
    protected ChannelUpstreamHandler createCoreHandler() {
        return new SMTPChannelUpstreamHandler(handlerChain, lmtpConfig, getLogger());
    }

    @Override
    protected OneToOneEncoder createEncoder() {
        return new SMTPResponseEncoder();
    }

    @Override
    protected SSLContext getSSLContext() {
        return null;
    }

    @Override
    protected boolean isSSLSocket() {
        return false;
    }

}
