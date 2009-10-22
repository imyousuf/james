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
package org.apache.james.socket.mina;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.socket.mina.filter.ConnectionFilter;
import org.apache.mailet.MailetContext;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.ssl.BogusTrustManagerFactory;
import org.apache.mina.filter.ssl.KeyStoreFactory;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * Abstract base class for Servers which want to use async io
 *
 */
public abstract class AbstractAsyncServer {
    /**
     * The default value for the connection backlog.
     */
    private static final int DEFAULT_BACKLOG = 5;
    
    /**
     * The default value for the connection timeout.
     */
    private static final int DEFAULT_TIMEOUT = 5* 60 * 1000;

    /**
     * The name of the parameter defining the connection timeout.
     */
    private static final String TIMEOUT_NAME = "connectiontimeout";

    /**
     * The name of the parameter defining the connection backlog.
     */
    private static final String BACKLOG_NAME = "connectionBacklog";

    /**
     * The name of the parameter defining the service hello name.
     */
    public static final String HELLO_NAME = "helloName";
    
    
    /**
     * The mailet context - we access it here to set the hello name for the Mailet API
     */
    private MailetContext mailetcontext;

    private FileSystem fileSystem;
    
    /**
     * The internal mail server service.
     */
    private MailServer mailServer;

    /** Loads instances */
    private LoaderService loader;

    private Log logger;

    private DNSService dns;

    private boolean enabled;

    private int connPerIP;

    private boolean useStartTLS;

    private int connectionLimit;

    private String helloName;
    
    private String keystore;

    private String secret;
    
    private int backlog;
    
    private InetAddress bindTo;

    private int port;

    private int timeout;

    private SslContextFactory contextFactory;

    private HierarchicalConfiguration config;;

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
    
    @Resource(name="org.apache.james.api.dnsservice.DNSService")
    public final void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    
    @Resource(name="org.apache.james.services.FileSystem")
    public final void setFileSystem(FileSystem filesystem) {
        this.fileSystem = filesystem;
    }
    
    @Resource(name="org.apache.james.services.MailServer")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    @Resource(name="org.apache.mailet.MailetContext")
    public final void setMailetContext(MailetContext mailetcontext) {
        this.mailetcontext = mailetcontext;
    }

    @Resource(name="org.apache.commons.logging.Log")
    public final void setLog(Log logger) {
       this.logger = logger;
    }

    @Resource(name="org.apache.commons.configuration.Configuration")
    public final void setConfiguration(HierarchicalConfiguration config) {
        this.config = config;
    }
    
    
    @PostConstruct
    public void init() throws Exception {
        configure(config);
        doConfigure(config);
        if (isEnabled()) {
            preInit();
            buildSSLContextFactory();
            SocketAcceptor acceptor = new NioSocketAcceptor();  
            acceptor.setFilterChainBuilder(createIoFilterChainBuilder());
            acceptor.setBacklog(backlog);
            acceptor.setReuseAddress(true);
            acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, timeout );
            acceptor.setHandler(createIoHandler());
            acceptor.bind(new InetSocketAddress(bindTo,port));
        }
    }

    /**
     * This method is called on init of the Server. Subclasses should override this method to init stuff
     *
     * @throws Exception 
     */
    protected void preInit() throws Exception {
        // override me
    }
    
    protected void doConfigure(HierarchicalConfiguration config) throws Exception {
        // override me
    }

    /**
     * Return the DNSService
     * 
     * @return dns
     */
    protected DNSService getDNSService() {
        return dns;
    }
    
    /**
     * Return the MailServer
     * 
     * @return mailServer
     */
    protected MailServer getMailServer() {
        return mailServer;
    }
    
    /**
     * Return the MailetContext
     * 
     * @return mailetContext
     */
    protected MailetContext getMailetContext() {
        return mailetcontext;
    }
    
    /**
     * Return the FileSystem
     * 
     * @return fileSystem
     */
    protected FileSystem getFileSystem() {
        return fileSystem;
    }
    

    private void configure(Configuration configuration) throws ConfigurationException {
        if ((configuration instanceof HierarchicalConfiguration) == false) throw new ConfigurationException("Configuration must extend HierarchicalConfiguration");
       
        Configuration handlerConfiguration = ((HierarchicalConfiguration)configuration).configurationAt("handler");

        enabled = configuration.getBoolean("/ @enabled", true);
        
        final Log logger = getLogger();
        if (!enabled) {
          logger.info(getServiceType() + " disabled by configuration");
          return;
        }

        
        /*
        boolean streamdump=handlerConfiguration.getChild("streamdump").getAttributeAsBoolean("enabled", false);
        String streamdumpDir=streamdump ? handlerConfiguration.getChild("streamdump").getAttribute("directory", null) : null;
        setStreamDumpDir(streamdumpDir);
        */

        port = configuration.getInt("port",getDefaultPort());

     

        StringBuilder infoBuffer;
        

        try {
            final String bindAddress = configuration.getString("bind",null);
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

        timeout = handlerConfiguration.getInt(TIMEOUT_NAME,DEFAULT_TIMEOUT);

        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" handler connection timeout is: ")
                    .append(timeout);
        logger.info(infoBuffer.toString());

        backlog = configuration.getInt(BACKLOG_NAME,DEFAULT_BACKLOG);

        infoBuffer =
                    new StringBuilder(64)
                    .append(getServiceType())
                    .append(" connection backlog is: ")
                    .append(backlog);
        logger.info(infoBuffer.toString());

        
        String connectionLimitString = configuration.getString("connectionLimit",null);
        if (connectionLimitString != null) {
            try {
                connectionLimit = new Integer(connectionLimitString);
            } catch (NumberFormatException nfe) {
                logger.error("Connection limit value is not properly formatted.", nfe);
            }
            if (connectionLimit < 0) {
                logger.error("Connection limit value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            } else if (connectionLimit > 0){
                infoBuffer = new StringBuilder(128)
                .append(getServiceType())
                .append(" will allow a maximum of ")
                .append(connectionLimitString)
                .append(" connections.");
                logger.info(infoBuffer.toString());
            }
        } 
       
        String connectionLimitPerIP = handlerConfiguration.getString("connectionLimitPerIP",null);
        if (connectionLimitPerIP != null) {
            try {
            connPerIP = new Integer(connectionLimitPerIP).intValue();
            } catch (NumberFormatException nfe) {
                logger.error("Connection limit per IP value is not properly formatted.", nfe);
            }
            if (connPerIP < 0) {
                logger.error("Connection limit per IP value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            } else if (connPerIP > 0){
                infoBuffer = new StringBuilder(128)
                .append(getServiceType())
                .append(" will allow a maximum of ")
                .append(connPerIP)
                .append(" per IP connections for " +getServiceType());
                logger.info(infoBuffer.toString());
            }
        }
       

        useStartTLS = configuration.getBoolean("startTLS/ @enable", false);

        if (useStartTLS) {
            keystore = configuration.getString("startTLS/keystore", null);
            if (keystore == null) {
                throw new ConfigurationException("keystore needs to get configured");
            }
            secret = configuration.getString("startTLS/secret","");
        }
             
    }

    
    /**
     * Configure the helloName for the given Configuration
     * 
     * @param handlerConfiguration
     */
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

        boolean autodetect = handlerConfiguration.getBoolean(HELLO_NAME + "/ @autodetect", true);
        if (autodetect) {
            helloName = hostName;
        } else {
            // Should we use the defaultdomain here ?
            helloName = handlerConfiguration.getString(HELLO_NAME + "/localhost");
        }

        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" handler hello name is: ")
                    .append(helloName);
        getLogger().info(infoBuffer.toString());
    }
    
    /**
     * Return the port this server will listen on
     * 
     * @return port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Return the logger
     * 
     * @return logger
     */
    protected Log getLogger() {
        return logger;
    }
    
    /**
     * Return if the server is enabled by the configuration
     * 
     * @return enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Return helloName for this server
     * 
     * @return helloName
     */
    public String getHelloName() {
        return helloName;
    }
    
    
    /**
     * Return if startTLS is supported by this server
     * 
     * @return startTlsSupported
     */
    protected boolean isStartTLSSupported() {
        return useStartTLS;
    }
    
    /**
     * Build the SslContextFactory
     * 
     * @throws Exception
     */
    private void buildSSLContextFactory() throws Exception{
        if (useStartTLS) {
            KeyStoreFactory kfactory = new KeyStoreFactory();
            kfactory.setDataFile(fileSystem.getFile(keystore));
            kfactory.setPassword(secret);
            
            contextFactory = new SslContextFactory();
            contextFactory.setKeyManagerFactoryKeyStore(kfactory.newInstance());
            contextFactory.setKeyManagerFactoryAlgorithm("SunX509");
            contextFactory.setTrustManagerFactory(new BogusTrustManagerFactory());
            contextFactory.setKeyManagerFactoryKeyStorePassword(secret);
        }
    }
    
    /**
     * Createh IoHandler to use by this Server implementation
     * 
     * @return ioHandler
     */
    protected abstract IoHandler createIoHandler();
    
    /**
     * Return the SslContextFactory which was created for this service. 
     * 
     * @return contextFactory
     */
    protected SslContextFactory getSslContextFactory() {
        return contextFactory;
    }
    
    
    /**
     * Create IoFilterChainBuilder which will get used for the Acceptor. 
     * The builder will contain a ProtocalCodecFilter which handles Line based Protocols and
     * a ConnectionFilter which limit the connection count / connection count per ip.
     * 
     * Developers should override this to add more filters to the chain.
     * 
     * @return ioFilterChainBuilder
     */
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        ProtocolCodecFilter codecFactory = new ProtocolCodecFilter(new TextLineCodecFactory());
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        builder.addLast("protocolCodecFactory", codecFactory);
        builder.addLast("connectionFilter", new ConnectionFilter(getLogger(), connectionLimit, connPerIP));
        return builder;
    }
    
    /**
     * Return the default port which will get used for this server if non is specify in the configuration
     * 
     * @return port
     */
    protected abstract int getDefaultPort();
    
    /**
     * Return textual representation of the service this server provide
     * 
     * @return serviceType
     */
    protected abstract String getServiceType();
}
