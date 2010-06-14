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
package org.apache.james.smtpserver.netty;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.dnsservice.util.NetMatcher;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.impl.AbstractSSLAwareChannelPipelineFactory;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPServerMBean;
import org.apache.james.services.MailServer;
import org.apache.james.socket.netty.AbstractConfigurableAsyncServer;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * NIO SMTPServer which use Netty
 * 
 *
 */
public class NioSMTPServer extends AbstractConfigurableAsyncServer implements SMTPServerMBean{

    
    /**
     * The handler chain - SMTPhandlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     * Constructed during initialisation to allow dependency injection.
     */
    private ProtocolHandlerChain handlerChain;

    /**
     * Whether authentication is required to use
     * this SMTP server.
     */
    private final static int AUTH_DISABLED = 0;
    private final static int AUTH_REQUIRED = 1;
    private final static int AUTH_ANNOUNCE = 2;
    private int authRequired = AUTH_DISABLED;

    /**
     * Whether the server needs helo to be send first
     */
    private boolean heloEhloEnforcement = false;

    /**
     * SMTPGreeting to use 
     */
    private String smtpGreeting = null;

    /**
     * This is a Network Matcher that should be configured to contain
     * authorized networks that bypass SMTP AUTH requirements.
     */
    private NetMatcher authorizedNetworks = null;

    /**
     * The maximum message size allowed by this SMTP server.  The default
     * value, 0, means no limit.
     */
    private long maxMessageSize = 0;

    /**
     * The number of bytes to read before resetting
     * the connection timeout timer.  Defaults to
     * 20 KB.
     */
    private int lengthReset = 20 * 1024;

    /**
     * The configuration data to be passed to the handler
     */
    private final SMTPConfiguration theConfigData = new SMTPHandlerConfigurationDataImpl();

    private boolean addressBracketsEnforcement = true;

    private MailServer mailServer;

    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    

    public void setProtocolHandlerChain(ProtocolHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }

    
    public void doConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        if (isEnabled()) {
            HierarchicalConfiguration handlerConfiguration = configuration.configurationAt("handler");
            String authRequiredString = handlerConfiguration.getString("authRequired","false").trim().toLowerCase();
            if (authRequiredString.equals("true")) authRequired = AUTH_REQUIRED;
            else if (authRequiredString.equals("announce")) authRequired = AUTH_ANNOUNCE;
            else authRequired = AUTH_DISABLED;
            if (authRequired != AUTH_DISABLED) {
                getLogger().info("This SMTP server requires authentication.");
            } else {
                getLogger().info("This SMTP server does not require authentication.");
            }

            String authorizedAddresses = handlerConfiguration.getString("authorizedAddresses",null);
            if (authRequired == AUTH_DISABLED && authorizedAddresses == null) {
                /* if SMTP AUTH is not requred then we will use
                 * authorizedAddresses to determine whether or not to
                 * relay e-mail.  Therefore if SMTP AUTH is not
                 * required, we will not relay e-mail unless the
                 * sending IP address is authorized.
                 *
                 * Since this is a change in behavior for James v2,
                 * create a default authorizedAddresses network of
                 * 0.0.0.0/0, which matches all possible addresses, thus
                 * preserving the current behavior.
                 *
                 * James v3 should require the <authorizedAddresses>
                 * element.
                 */
                authorizedAddresses = "0.0.0.0/0.0.0.0";
            }

            if (authorizedAddresses != null) {
                java.util.StringTokenizer st = new java.util.StringTokenizer(authorizedAddresses, ", ", false);
                java.util.Collection<String> networks = new java.util.ArrayList<String>();
                while (st.hasMoreTokens()) {
                    String addr = st.nextToken();
                    networks.add(addr);
                }
                authorizedNetworks = new NetMatcher(networks, getDNSService());
            }

            if (authorizedNetworks != null) {
                getLogger().info("Authorized addresses: " + authorizedNetworks.toString());
            }

            // get the message size limit from the conf file and multiply
            // by 1024, to put it in bytes
            maxMessageSize = handlerConfiguration.getLong( "maxmessagesize",maxMessageSize ) * 1024;
            if (maxMessageSize > 0) {
                getLogger().info("The maximum allowed message size is " + maxMessageSize + " bytes.");
            } else {
                getLogger().info("No maximum message size is enforced for this server.");
            }
            // How many bytes to read before updating the timer that data is being transfered
            lengthReset = configuration.getInt("lengthReset", lengthReset);
            if (lengthReset <= 0) {
                throw new ConfigurationException("The configured value for the idle timeout reset, " + lengthReset + ", is not valid.");
            }
            if (getLogger().isInfoEnabled()) {
                getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
            }

            heloEhloEnforcement = handlerConfiguration.getBoolean("heloEhloEnforcement",true);

            if (authRequiredString.equals("true")) authRequired = AUTH_REQUIRED;

            // get the smtpGreeting
            smtpGreeting = handlerConfiguration.getString("smtpGreeting",null);

            addressBracketsEnforcement = handlerConfiguration.getBoolean("addressBracketsEnforcement",true);
        }
    }

    /**
     * @see org.apache.james.socket.AbstractConfigurableAsyncServer.AbstractAsyncServer#getDefaultPort()
     */
    protected int getDefaultPort() {
        return 25;
    }

    /**
     * @see org.apache.james.core.AbstractProtocolServer#getServiceType()
     */
    public String getServiceType() {
        return "SMTP Service";
    }


    /**
     * A class to provide SMTP handler configuration to the handlers
     */
    private class SMTPHandlerConfigurationDataImpl implements SMTPConfiguration {

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getHelloName()
         */
        public String getHelloName() {
            if (NioSMTPServer.this.getHelloName() == null) {
                return NioSMTPServer.this.mailServer.getHelloName();
            } else {
                return NioSMTPServer.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getResetLength()
         */
        public int getResetLength() {
            return NioSMTPServer.this.lengthReset;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return NioSMTPServer.this.maxMessageSize;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isAuthSupported(String)
         */
        public boolean isRelayingAllowed(String remoteIP) {
            boolean relayingAllowed = false;
            if (authorizedNetworks != null) {
                relayingAllowed = NioSMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return relayingAllowed;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#useHeloEhloEnforcement()
         */
        public boolean useHeloEhloEnforcement() {
            return NioSMTPServer.this.heloEhloEnforcement;
        }


        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return NioSMTPServer.this.smtpGreeting;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return NioSMTPServer.this.addressBracketsEnforcement;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isAuthRequired(java.lang.String)
         */
        public boolean isAuthRequired(String remoteIP) {
            if (NioSMTPServer.this.authRequired == AUTH_ANNOUNCE) return true;
            boolean authRequired = NioSMTPServer.this.authRequired != AUTH_DISABLED;
            if (authorizedNetworks != null) {
                authRequired = authRequired && !NioSMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return authRequired;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isStartTLSSupported()
         */
        public boolean isStartTLSSupported() {
            return NioSMTPServer.this.isStartTLSSupported();
        }

    }
    
    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new SMTPChannelPipelineFactory(getTimeout(), connectionLimit, connPerIP, group);
    }
    
    
    private final class SMTPChannelPipelineFactory extends AbstractSSLAwareChannelPipelineFactory {

        public SMTPChannelPipelineFactory(int timeout, int maxConnections,
                int maxConnectsPerIp, ChannelGroup group) {
            super(timeout, maxConnections, maxConnectsPerIp, group);
        }

        @Override
        protected SSLContext getSSLContext() {
            return NioSMTPServer.this.getSSLContext();
        }

        @Override
        protected boolean isSSLSocket() {
            return  NioSMTPServer.this.isSSLSocket();
        }

        @Override
        protected OneToOneEncoder createEncoder() {
            return new SMTPResponseEncoder();
        }

        @Override
        protected ChannelUpstreamHandler createHandler() {
            return new SMTPChannelUpstreamHandler(handlerChain, theConfigData, getLogger(), getSSLContext());
        }
        
    }
}
