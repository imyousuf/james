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



package org.apache.james.smtpserver.mina;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.dnsservice.util.NetMatcher;
import org.apache.james.smtpserver.mina.filter.SMTPValidationFilter;
import org.apache.james.smtpserver.mina.filter.SMTPResponseFilter;
import org.apache.james.smtpserver.protocol.SMTPConfiguration;
import org.apache.james.smtpserver.protocol.SMTPHandlerChain;
import org.apache.james.smtpserver.protocol.SMTPServerMBean;
import org.apache.james.smtpserver.protocol.core.CoreCmdHandlerLoader;
import org.apache.james.socket.configuration.JamesConfiguration;
import org.apache.james.socket.mina.AbstractAsyncServer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;

/**
 * <p>Accepts SMTP connections on a server socket and dispatches them to SMTPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing SMTP specific configuration.</p>
 *
 * @version 1.1.0, 06/02/2001
 */
/*
 * IMPORTANT: AsyncSMTPServer extends AbstractAsyncServer.  If you implement ANY
 * lifecycle methods, you MUST call super.<method> as well.
 */
public class AsyncSMTPServer extends AbstractAsyncServer implements SMTPServerMBean {
   
    /**
     * The handler chain - SMTPhandlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     * Constructed during initialisation to allow dependency injection.
     */
    private SMTPHandlerChain handlerChain;

   
    /** Cached configuration data for handler */
    private Configuration handlerConfiguration;

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
    private SMTPConfiguration theConfigData
    = new SMTPHandlerConfigurationDataImpl();

    private boolean addressBracketsEnforcement = true;


    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        super.configure(configuration);
        if (isEnabled()) {
            handlerConfiguration = configuration.getChild("handler");
            String authRequiredString = handlerConfiguration.getChild("authRequired").getValue("false").trim().toLowerCase();
            if (authRequiredString.equals("true")) authRequired = AUTH_REQUIRED;
            else if (authRequiredString.equals("announce")) authRequired = AUTH_ANNOUNCE;
            else authRequired = AUTH_DISABLED;
            if (authRequired != AUTH_DISABLED) {
                getLogger().info("This SMTP server requires authentication.");
            } else {
                getLogger().info("This SMTP server does not require authentication.");
            }

            String authorizedAddresses = handlerConfiguration.getChild("authorizedAddresses").getValue(null);
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
            maxMessageSize = handlerConfiguration.getChild( "maxmessagesize" ).getValueAsLong( maxMessageSize ) * 1024;
            if (maxMessageSize > 0) {
                getLogger().info("The maximum allowed message size is " + maxMessageSize + " bytes.");
            } else {
                getLogger().info("No maximum message size is enforced for this server.");
            }
            // How many bytes to read before updating the timer that data is being transfered
            lengthReset = configuration.getChild("lengthReset").getValueAsInteger(lengthReset);
            if (lengthReset <= 0) {
                throw new ConfigurationException("The configured value for the idle timeout reset, " + lengthReset + ", is not valid.");
            }
            if (getLogger().isInfoEnabled()) {
                getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
            }

            heloEhloEnforcement = handlerConfiguration.getChild("heloEhloEnforcement").getValueAsBoolean(true);

            if (authRequiredString.equals("true")) authRequired = AUTH_REQUIRED;

            // get the smtpGreeting
            smtpGreeting = handlerConfiguration.getChild("smtpGreeting").getValue(null);

            addressBracketsEnforcement = handlerConfiguration.getChild("addressBracketsEnforcement").getValueAsBoolean(true);
        }
    }
    
    
    /**
     * Prepare the handlerchain
     * 
     * @throws Exception
     */
    private void prepareHandlerChain() throws Exception {
        handlerChain = getLoader().load(SMTPHandlerChain.class);
                      
        //set the logger
        handlerChain.setLog(new AvalonLogger(getLogger()));
        
        //read from the XML configuration and create and configure each of the handlers
        JamesConfiguration jamesConfiguration = new JamesConfiguration(handlerConfiguration.getChild("handlerchain"));
        if (jamesConfiguration.getProperty("coreHandlersPackage") == null)
            jamesConfiguration.addProperty("coreHandlersPackage", CoreCmdHandlerLoader.class.getName());
        handlerChain.configure(jamesConfiguration);
    }


    /**
     * @see org.apache.james.socket.mina.AbstractAsyncServer#preInit()
     */
    protected void preInit() throws Exception {
        prepareHandlerChain();
    }



    /**
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getDefaultPort()
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
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getHelloName()
         */
        public String getHelloName() {
            if (AsyncSMTPServer.this.getHelloName() == null) {
                return AsyncSMTPServer.this.getMailServer().getHelloName();
            } else {
                return AsyncSMTPServer.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getResetLength()
         */
        public int getResetLength() {
            return AsyncSMTPServer.this.lengthReset;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return AsyncSMTPServer.this.maxMessageSize;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#isAuthSupported(String)
         */
        public boolean isRelayingAllowed(String remoteIP) {
            boolean relayingAllowed = false;
            if (authorizedNetworks != null) {
                relayingAllowed = AsyncSMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return relayingAllowed;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#useHeloEhloEnforcement()
         */
        public boolean useHeloEhloEnforcement() {
            return AsyncSMTPServer.this.heloEhloEnforcement;
        }


        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return AsyncSMTPServer.this.smtpGreeting;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return AsyncSMTPServer.this.addressBracketsEnforcement;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#isAuthRequired(java.lang.String)
         */
        public boolean isAuthRequired(String remoteIP) {
            if (AsyncSMTPServer.this.authRequired == AUTH_ANNOUNCE) return true;
            boolean authRequired = AsyncSMTPServer.this.authRequired != AUTH_DISABLED;
            if (authorizedNetworks != null) {
                authRequired = authRequired && !AsyncSMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return authRequired;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#isStartTLSSupported()
         */
		public boolean isStartTLSSupported() {
			return AsyncSMTPServer.this.isStartTLSSupported();
		}

    }
    
    /**
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.SMTPServerMBean#getNetworkInterface()
     */
    public String getNetworkInterface() {
        return "unkown";
    }

    /**
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.SMTPServerMBean#getSocketType()
     */
    public String getSocketType() {
        return "plain";
    }


    /**
     * @see org.apache.james.socket.mina.AbstractAsyncServer#createIoFilterChainBuilder()
     */
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        DefaultIoFilterChainBuilder builder = super.createIoFilterChainBuilder();
        
        // response and validation filter to the chain
        builder.addLast("smtpResponseFilter", new SMTPResponseFilter());
        builder.addLast("requestValidationFilter", new SMTPValidationFilter(new AvalonLogger(getLogger())));
        return builder;
    }



    /**
     * @see org.apache.james.socket.mina.AbstractAsyncServer#createIoHandler()
     */
    protected IoHandler createIoHandler() {
        Log logger = new AvalonLogger(getLogger());
        return new SMTPIoHandler(handlerChain, theConfigData,logger,getSslContextFactory());
    }
    
}
