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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.util.NetMatcher;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.integration.CoreCmdHandlerLoader;
import org.apache.james.smtpserver.protocol.SMTPConfiguration;
import org.apache.james.socket.AvalonProtocolServer;
import org.apache.james.socket.api.ProtocolHandler;
import org.apache.james.socket.api.ProtocolHandlerFactory;
import org.apache.james.socket.api.ProtocolServer;
import org.apache.james.socket.shared.ProtocolHandlerChainImpl;

/**
 * This is an test refactoring for SMTPServer where the avalon socket server
 * is a dependency and this class do not depend on it.
 * 
 * <p>Accepts SMTP connections on a server socket and dispatches them to SMTPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing SMTP specific configuration.</p>
 *
 * @version 1.1.0, 06/02/2001
 */
/*
 * IMPORTANT: SMTPServer extends AbstractJamesService.  If you implement ANY
 * lifecycle methods, you MUST call super.<method> as well.
 */
public class SMTPServerComposed implements ProtocolHandlerFactory {


    /**
     * The handler chain - SMTPhandlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     * Constructed during initialisation to allow dependency injection.
     */
    private ProtocolHandlerChainImpl handlerChain;
    /**
     * The internal mail server service.
     */
    private MailServer mailServer;

    /** Loads instances */
    private LoaderService loader;

    /** Cached configuration data for handler */
    private HierarchicalConfiguration handlerConfiguration;

    /**
     * The DNSService
     */
    private DNSService dnsService = null;

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
    
    private AvalonProtocolServer avalonProtocolServer;

    private Log log;
    private HierarchicalConfiguration configuration;
    
    /**
     * Gets the current instance loader.
     * @return the loader
     */
    public final LoaderService getLoader() {
        return loader;
    }

    /**
     * Sets the loader to be used for instances.
     * @param loader the loader to set, not null
     */
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoader(LoaderService loader) {
        this.loader = loader;
    }

    @Resource(name="org.apache.james.services.DNSService")
    public final void setDNSService(DNSService dns) {
        this.dnsService = dns;
    }

    @Resource(name="org.apache.james.socket.AvalonProtocolServer")
    public final void setAvalonProtocolServer(AvalonProtocolServer avalonProtocolServer) {
        this.avalonProtocolServer = avalonProtocolServer;
    }
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    
    @Resource(name="org.apache.commons.logging.Log")
    public void setLog(Log logger) {
        this.log = logger;
    }
    
    private void configure() throws ConfigurationException {
        if (avalonProtocolServer.isEnabled()) {

            handlerConfiguration = configuration.configurationAt("handler");
            String authRequiredString = handlerConfiguration.getString("authRequired", "false").trim().toLowerCase();
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
                authorizedNetworks = new NetMatcher(networks, dnsService);
            }

            if (authorizedNetworks != null) {
                getLogger().info("Authorized addresses: " + authorizedNetworks.toString());
            }

            // get the message size limit from the conf file and multiply
            // by 1024, to put it in bytes
            maxMessageSize = handlerConfiguration.getLong( "maxmessagesize", maxMessageSize ) * 1024;
            if (maxMessageSize > 0) {
                getLogger().info("The maximum allowed message size is " + maxMessageSize + " bytes.");
            } else {
                getLogger().info("No maximum message size is enforced for this server.");
            }
            // How many bytes to read before updating the timer that data is being transfered
            lengthReset = configuration.getInteger("lengthReset", lengthReset);
            if (lengthReset <= 0) {
                throw new ConfigurationException("The configured value for the idle timeout reset, " + lengthReset + ", is not valid.");
            }
            if (getLogger().isInfoEnabled()) {
                getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
            }

            heloEhloEnforcement = handlerConfiguration.getBoolean("heloEhloEnforcement",true);

            if (authRequiredString.equals("true")) authRequired = AUTH_REQUIRED;

            // get the smtpGreeting
            smtpGreeting = handlerConfiguration.getString("smtpGreeting", null);

            addressBracketsEnforcement = handlerConfiguration.getBoolean("addressBracketsEnforcement",true);
        }
    }

    protected Log getLogger() {
        return log;
    }

    private void prepareHandlerChain() throws Exception {
        handlerChain = loader.load(ProtocolHandlerChainImpl.class);
        
        //set the logger
        handlerChain.setLog(getLogger());
        
        //read from the XML configuration and create and configure each of the handlers
        HierarchicalConfiguration jamesConfiguration = handlerConfiguration.configurationAt("handlerchain");
        if (jamesConfiguration.getString("[@coreHandlersPackage]") == null)
            jamesConfiguration.addProperty("[@coreHandlersPackage]", CoreCmdHandlerLoader.class.getName());
        handlerChain.configure(jamesConfiguration);
    }

    /**
     * @see org.apache.james.core.AbstractProtocolServer#getDefaultPort()
     */
    public int getDefaultPort() {
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
            if (SMTPServerComposed.this.avalonProtocolServer.getHelloName() == null) {
                return SMTPServerComposed.this.mailServer.getHelloName();
            } else {
                return SMTPServerComposed.this.avalonProtocolServer.getHelloName();
            }
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getResetLength()
         */
        public int getResetLength() {
            return SMTPServerComposed.this.lengthReset;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return SMTPServerComposed.this.maxMessageSize;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#isAuthSupported(String)
         */
        public boolean isRelayingAllowed(String remoteIP) {
            boolean relayingAllowed = false;
            if (authorizedNetworks != null) {
                relayingAllowed = SMTPServerComposed.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return relayingAllowed;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#useHeloEhloEnforcement()
         */
        public boolean useHeloEhloEnforcement() {
            return SMTPServerComposed.this.heloEhloEnforcement;
        }


        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return SMTPServerComposed.this.smtpGreeting;
        }

        /**
         * @see org.apache.james.smtpserver.protocol.SMTPConfiguration#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return SMTPServerComposed.this.addressBracketsEnforcement;
        }

        public boolean isAuthRequired(String remoteIP) {
            if (SMTPServerComposed.this.authRequired == AUTH_ANNOUNCE) return true;
            boolean authRequired = SMTPServerComposed.this.authRequired != AUTH_DISABLED;
            if (authorizedNetworks != null) {
                authRequired = authRequired && !SMTPServerComposed.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return authRequired;
        }

		public boolean isStartTLSSupported() {
			return SMTPServerComposed.this.avalonProtocolServer.useStartTLS();
		}
        
        //TODO: IF we create here an interface to get DNSServer
        //      we should access it from the SMTPHandlers

    }

    /**
     * @see org.apache.james.socket.AbstractProtocolServer#newProtocolHandlerInstance()
     */
    public ProtocolHandler newProtocolHandlerInstance() {
        final SMTPHandler theHandler = new SMTPHandler(handlerChain, theConfigData);
        return theHandler;
    }

    
    public void doInit() throws Exception {
        // complete the initialization
        configure();
    }

    public void prepare(ProtocolServer server) throws Exception {
        prepareHandlerChain();
    }
    
}
