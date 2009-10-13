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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.annotation.Resource;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.Constants;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.util.NetMatcher;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.mina.RequestValidationFilter;
import org.apache.james.smtpserver.mina.SMTPCommandDispatcherIoHandler;
import org.apache.james.smtpserver.mina.SMTPResponseFilter;
import org.apache.james.socket.configuration.JamesConfiguration;
import org.apache.mailet.MailetContext;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

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
public class SMTPServer extends AbstractLogEnabled implements SMTPServerMBean, Serviceable, Initializable, Configurable {
    /**
     * The default value for the connection backlog.
     */
    protected static final int DEFAULT_BACKLOG = 5;
    
    /**
     * The default value for the connection timeout.
     */
    protected static final int DEFAULT_TIMEOUT = 5* 60 * 1000;

    /**
     * The name of the parameter defining the connection timeout.
     */
    protected static final String TIMEOUT_NAME = "connectiontimeout";

    /**
     * The name of the parameter defining the connection backlog.
     */
    protected static final String BACKLOG_NAME = "connectionBacklog";

    /**
     * The name of the parameter defining the service hello name.
     */
    public static final String HELLO_NAME = "helloName";
    
    /**
     * The handler chain - SMTPhandlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     * Constructed during initialisation to allow dependency injection.
     */
    private SMTPHandlerChain handlerChain;

    /**
     * The mailet context - we access it here to set the hello name for the Mailet API
     */
    private MailetContext mailetcontext;

    /**
     * The internal mail server service.
     */
    private MailServer mailServer;

    /** Loads instances */
    private LoaderService loader;

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

    private DNSService dns;

    public String helloName;

    private boolean enabled;

    private int port;

    private InetAddress bindTo;

    private int timeout;

    private int backlog;
    
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
    
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager manager ) throws ServiceException {
        mailetcontext = (MailetContext) manager.lookup("org.apache.mailet.MailetContext");
        mailServer = (MailServer) manager.lookup(MailServer.ROLE);
        dns = (DNSService) manager.lookup(DNSService.ROLE);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        enabled = configuration.getAttributeAsBoolean("enabled", true);
        final Logger logger = getLogger();
        if (!enabled) {
          logger.info(getServiceType() + " disabled by configuration");
          return;
        }

        Configuration handlerConfiguration = configuration.getChild("handler");

        
        /*
        boolean streamdump=handlerConfiguration.getChild("streamdump").getAttributeAsBoolean("enabled", false);
        String streamdumpDir=streamdump ? handlerConfiguration.getChild("streamdump").getAttribute("directory", null) : null;
        setStreamDumpDir(streamdumpDir);
        */

        port = configuration.getChild("port").getValueAsInteger(25);

     

        StringBuilder infoBuffer;
        

        try {
            final String bindAddress = configuration.getChild("bind").getValue(null);
            if( null != bindAddress ) {
                bindTo = InetAddress.getByName(bindAddress);
                infoBuffer =
                    new StringBuilder(64)
                            .append(getServiceType())
                            .append(" bound to: ")
                            .append(bindTo);
                logger.info(infoBuffer.toString());
            }
        }
        catch( final UnknownHostException unhe ) {
            throw new ConfigurationException( "Malformed bind parameter in configuration of service " + getServiceType(), unhe );
        }

        configureHelloName(handlerConfiguration);

        timeout = handlerConfiguration.getChild(TIMEOUT_NAME).getValueAsInteger(DEFAULT_TIMEOUT);

        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" handler connection timeout is: ")
                    .append(timeout);
        logger.info(infoBuffer.toString());

        backlog = configuration.getChild(BACKLOG_NAME).getValueAsInteger(DEFAULT_BACKLOG);

        infoBuffer =
                    new StringBuilder(64)
                    .append(getServiceType())
                    .append(" connection backlog is: ")
                    .append(backlog);
        logger.info(infoBuffer.toString());

        /*
        String connectionLimitString = configuration.getChild("connectionLimit").getValue(null);
        if (connectionLimitString != null) {
            try {
                connectionLimit = new Integer(connectionLimitString);
            } catch (NumberFormatException nfe) {
                logger.error("Connection limit value is not properly formatted.", nfe);
            }
            if (connectionLimit.intValue() < 0) {
                logger.error("Connection limit value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            }
        } else {
            connectionLimit = new Integer(connectionManager.getMaximumNumberOfOpenConnections());
        }
        infoBuffer = new StringBuilder(128)
            .append(getServiceType())
            .append(" will allow a maximum of ")
            .append(connectionLimit.intValue())
            .append(" connections.");
        logger.info(infoBuffer.toString());
        
        String connectionLimitPerIP = conf.getChild("connectionLimitPerIP").getValue(null);
        if (connectionLimitPerIP != null) {
            try {
            connPerIP = new Integer(connectionLimitPerIP).intValue();
            connPerIPConfigured = true;
            } catch (NumberFormatException nfe) {
                logger.error("Connection limit per IP value is not properly formatted.", nfe);
            }
            if (connPerIP < 0) {
                logger.error("Connection limit per IP value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            }
        } else {
            connPerIP = connectionManager.getMaximumNumberOfOpenConnectionsPerIP();
        }
        infoBuffer = new StringBuilder(128)
            .append(getServiceType())
            .append(" will allow a maximum of ")
            .append(connPerIP)
            .append(" per IP connections for " +getServiceType());
        logger.info(infoBuffer.toString());
        
        Configuration tlsConfig = conf.getChild("startTLS");
        if (tlsConfig != null) {
            useStartTLS = tlsConfig.getAttributeAsBoolean("enable", false);
            
            if (useStartTLS) {
                keystore = tlsConfig.getChild("keystore").getValue(null);
                if (keystore == null) {
                    throw new ConfigurationException("keystore needs to get configured");
                }
                secret = tlsConfig.getChild("secret").getValue("");
                loadJCEProviders(tlsConfig, getLogger());
            }
        }
        */
        String hello = (String) mailetcontext.getAttribute(Constants.HELLO_NAME);

        if (configuration.getAttributeAsBoolean("enabled")) {
            // TODO Remove this in next not backwards compatible release!
            if (hello == null) mailetcontext.setAttribute(Constants.HELLO_NAME, helloName);

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
                authorizedNetworks = new NetMatcher(networks, dns);
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
        } else {
            // TODO Remove this in next not backwards compatible release!
            if (hello == null) mailetcontext.setAttribute(Constants.HELLO_NAME, "localhost");
        }
        
        this.handlerConfiguration = handlerConfiguration;
    }
    
    private void configureHelloName(Configuration handlerConfiguration) {
        StringBuilder infoBuffer;
        String hostName = null;
        try {
            hostName = dns.getHostName(dns.getLocalHost());
        } catch (UnknownHostException ue) {
            hostName = "localhost";
        }

        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" is running on: ")
                    .append(hostName);
        getLogger().info(infoBuffer.toString());

        Configuration helloConf = handlerConfiguration.getChild(HELLO_NAME);
 
        if (helloConf != null) {
            boolean autodetect = helloConf.getAttributeAsBoolean("autodetect", true);
            if (autodetect) {
                helloName = hostName;
            } else {
                // Should we use the defaultdomain here ?
                helloName = helloConf.getValue("localhost");
            }
        } else {
            helloName = null;
        }
        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" handler hello name is: ")
                    .append(helloName);
        getLogger().info(infoBuffer.toString());
    }
    private void prepareHandlerChain() throws Exception {
        handlerChain = loader.load(SMTPHandlerChain.class);
        
        //set the logger
        handlerChain.setLog(new AvalonLogger(getLogger()));
        
        //read from the XML configuration and create and configure each of the handlers
        handlerChain.configure(new JamesConfiguration(handlerConfiguration.getChild("handlerchain")));
    }


    /**
     * @see org.apache.james.core.AbstractProtocolServer#getDefaultPort()
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
         * @see org.apache.james.smtpserver.SMTPConfiguration#getHelloName()
         */
        public String getHelloName() {
            if (SMTPServer.this.helloName == null) {
                return SMTPServer.this.mailServer.getHelloName();
            } else {
                return SMTPServer.this.helloName;
            }
        }

        /**
         * @see org.apache.james.smtpserver.SMTPConfiguration#getResetLength()
         */
        public int getResetLength() {
            return SMTPServer.this.lengthReset;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPConfiguration#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return SMTPServer.this.maxMessageSize;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPConfiguration#isAuthSupported(String)
         */
        public boolean isRelayingAllowed(String remoteIP) {
            boolean relayingAllowed = false;
            if (authorizedNetworks != null) {
                relayingAllowed = SMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return relayingAllowed;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPConfiguration#useHeloEhloEnforcement()
         */
        public boolean useHeloEhloEnforcement() {
            return SMTPServer.this.heloEhloEnforcement;
        }


        /**
         * @see org.apache.james.smtpserver.SMTPConfiguration#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return SMTPServer.this.smtpGreeting;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPConfiguration#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return SMTPServer.this.addressBracketsEnforcement;
        }

        public boolean isAuthRequired(String remoteIP) {
            if (SMTPServer.this.authRequired == AUTH_ANNOUNCE) return true;
            boolean authRequired = SMTPServer.this.authRequired != AUTH_DISABLED;
            if (authorizedNetworks != null) {
                authRequired = authRequired && !SMTPServer.this.authorizedNetworks.matchInetNetwork(remoteIP);
            }
            return authRequired;
        }

		public boolean isStartTLSSupported() {
		    //TODO: FIX ME
			return false;
		}
        
        //TODO: IF we create here an interface to get DNSServer
        //      we should access it from the SMTPHandlers

    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        prepareHandlerChain();
        
        ProtocolCodecFilter codecFactory = new ProtocolCodecFilter(new TextLineCodecFactory());
        SocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new SMTPCommandDispatcherIoHandler(handlerChain, theConfigData,new AvalonLogger(getLogger())));
        
        acceptor.getFilterChain().addLast("loggingFilter",new LoggingFilter());
        acceptor.getFilterChain().addLast("protocolCodecFactory", codecFactory);
        acceptor.getFilterChain().addLast("smtpResponseFilter", new SMTPResponseFilter());
        acceptor.getFilterChain().addLast("requestValidationFilter", new RequestValidationFilter(new AvalonLogger(getLogger())));
        acceptor.setBacklog(backlog);
        acceptor.setReuseAddress(true);
        acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, timeout );
        acceptor.bind(new InetSocketAddress(bindTo,port));
    }

    public String getNetworkInterface() {
        return null;
    }

    public int getPort() {
        return port;
    }

    public String getSocketType() {
        return "plain";
    }

    public boolean isEnabled() {
        return enabled;
    }
    
}
