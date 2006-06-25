/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
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

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.services.DNSServer;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.util.NetMatcher;
import org.apache.mailet.MailetContext;

/**
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
public class SMTPServer extends AbstractJamesService implements SMTPServerMBean, Contextualizable {


    /**
     * The handler chain - SMTPhandlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     */
    SMTPHandlerChain handlerChain = new SMTPHandlerChain();

    /**
     * The mailet context - we access it here to set the hello name for the Mailet API
     */
    MailetContext mailetcontext;

    /**
     * The user repository for this server - used to authenticate
     * users.
     */
    private UsersRepository users;

    /**
     * The internal mail server service.
     */
    private MailServer mailServer;
    
    /**
     * The DNSServer to use for queries
     */
    private DNSServer dnsServer;
    
    /**
     * Whether authentication is required to use
     * this SMTP server.
     */
    private final static int AUTH_DISABLED = 0;
    private final static int AUTH_REQUIRED = 1;
    private final static int AUTH_ANNOUNCE = 2;
    private int authRequired = AUTH_DISABLED;

    /**
     * Whether the server verifies that the user
     * actually sending an email matches the
     * authentication credentials attached to the
     * SMTP interaction.
     */
    private boolean verifyIdentity = false;

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
    private SMTPHandlerConfigurationData theConfigData
        = new SMTPHandlerConfigurationDataImpl();

    private ServiceManager serviceManager;


    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager manager ) throws ServiceException {
        super.service( manager );
        serviceManager = manager;
        mailetcontext = (MailetContext) manager.lookup("org.apache.mailet.MailetContext");
        mailServer = (MailServer) manager.lookup(MailServer.ROLE);
        users = (UsersRepository) manager.lookup(UsersRepository.ROLE);
        dnsServer = (DNSServer) manager.lookup(DNSServer.ROLE); 
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        super.configure(configuration);
        if (isEnabled()) {
            mailetcontext.setAttribute(Constants.HELLO_NAME, helloName);
            Configuration handlerConfiguration = configuration.getChild("handler");
            String authRequiredString = handlerConfiguration.getChild("authRequired").getValue("false").trim().toLowerCase();
            if (authRequiredString.equals("true")) authRequired = AUTH_REQUIRED;
            else if (authRequiredString.equals("announce")) authRequired = AUTH_ANNOUNCE;
            else authRequired = AUTH_DISABLED;
            verifyIdentity = handlerConfiguration.getChild("verifyIdentity").getValueAsBoolean(false);
            if (authRequired != AUTH_DISABLED) {
                if (verifyIdentity) {
                    getLogger().info("This SMTP server requires authentication and verifies that the authentication credentials match the sender address.");
                } else {
                    getLogger().info("This SMTP server requires authentication, but doesn't verify that the authentication credentials match the sender address.");
                }
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
                java.util.Collection networks = new java.util.ArrayList();
                while (st.hasMoreTokens()) {
                    String addr = st.nextToken();
                    networks.add(addr);
                }
                authorizedNetworks = new NetMatcher(networks,dnsServer);
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

            //set the logger
            ContainerUtil.enableLogging(handlerChain,getLogger());
           
            try {
                ContainerUtil.service(handlerChain,serviceManager);
            } catch (ServiceException e) {
                if (getLogger().isErrorEnabled()) {
                    getLogger().error("Failed to service handlerChain",e);
                }
                throw new ConfigurationException("Failed to service handlerChain");
            }
            
            //read from the XML configuration and create and configure each of the handlers
            ContainerUtil.configure(handlerChain,handlerConfiguration.getChild("handlerchain"));

        } else {
            mailetcontext.setAttribute(Constants.HELLO_NAME, "localhost");
        }
        theHandlerFactory = new SMTPHandlerFactory();
    }
    
    /**
     * @see org.apache.james.core.AbstractJamesService#initialize()
     */
    public void initialize() throws Exception {
        super.initialize();
        ContainerUtil.initialize(handlerChain);
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize(final Context context) {
        super.contextualize(context);
        try {
            ContainerUtil.contextualize(handlerChain, context);
        } catch (ContextException e) {

            // This is just a hack.. the overridden contextualize method not
            // support
            // of throw an exception. But we need it
            throw new IllegalStateException(e.getMessage());
        }

    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getDefaultPort()
     */
     protected int getDefaultPort() {
        return 25;
     }

    /**
     * @see org.apache.james.core.AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "SMTP Service";
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#newHandler()
     */
    protected ConnectionHandler newHandler()
            throws Exception {
        
        SMTPHandler theHandler = (SMTPHandler) super.newHandler();

        //pass the handler chain to every SMTPhandler
        theHandler.setHandlerChain(handlerChain);

        return theHandler;
    }

    /**
     * The factory for producing handlers.
     */
    private static class SMTPHandlerFactory
        implements ObjectFactory {

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception {
            return new SMTPHandler();
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass() {
            return SMTPHandler.class;
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#decommision(Object)
         */
        public void decommission( Object object ) throws Exception {
            return;
        }
    }

    /**
     * A class to provide SMTP handler configuration to the handlers
     */
    private class SMTPHandlerConfigurationDataImpl
        implements SMTPHandlerConfigurationData {

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            return SMTPServer.this.helloName;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return SMTPServer.this.lengthReset;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return SMTPServer.this.maxMessageSize;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#isAuthRequired(String)
         */
        public boolean isRelayingAllowed(String remoteIP) {
            boolean relayingAllowed = false;
            if (authorizedNetworks != null) {
                relayingAllowed = SMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return relayingAllowed;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#isAuthRequired(String)
         */
        public boolean isAuthRequired(String remoteIP) {
              if (SMTPServer.this.authRequired == AUTH_ANNOUNCE) return true;
            boolean authRequired = SMTPServer.this.authRequired != AUTH_DISABLED;
            if (authorizedNetworks != null) {
                authRequired = authRequired && !SMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return authRequired;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#isAuthRequired()
         */
        public boolean isAuthRequired() {
            return SMTPServer.this.authRequired != AUTH_DISABLED;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#isVerifyIdentity()
         */
        public boolean isVerifyIdentity() {
            return SMTPServer.this.verifyIdentity;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getMailServer()
         */
        public MailServer getMailServer() {
            return SMTPServer.this.mailServer;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository() {
            return SMTPServer.this.users;
        }
        
        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#useHeloEnforcement()
         */
        public boolean useHeloEhloEnforcement() {
            return SMTPServer.this.heloEhloEnforcement;
        }
        
        
        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return SMTPServer.this.smtpGreeting;
        }
        
        //TODO: IF we create here an interface to get DNSServer
        //      we should access it from the SMTPHandlers

    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getConfigurationData()
     */
    protected Object getConfigurationData() {
        return theConfigData;
    }
}
