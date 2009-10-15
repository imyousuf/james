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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.annotation.Resource;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.Constants;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.mina.filter.ConnectionFilter;
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
public abstract class AbstractAsyncServer implements LogEnabled, Initializable, Serviceable, Configurable{
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

    private Logger logger;

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

    private SslContextFactory contextFactory;;

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
    
    public void setFileSystem(FileSystem filesystem) {
        this.fileSystem = filesystem;
    }
    
    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    public void setMailetContext(MailetContext mailetcontext) {
        this.mailetcontext = mailetcontext;
    }

    
    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
       this.logger = logger;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
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

    /**
     * This method is called on init of the Server. Subclasses should override this method to init stuff
     * @throws Exception 
     */
    protected void preInit() throws Exception {
        // TODO Auto-generated method stub
        
    }

    protected DNSService getDNSService() {
        return dns;
    }
    
    protected MailServer getMailServer() {
        return mailServer;
    }
    
    protected MailetContext getMailetContext() {
        return mailetcontext;
    }
    
    protected FileSystem getFileSystem() {
        return fileSystem;
    }
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( final ServiceManager manager ) throws ServiceException {
        setMailetContext((MailetContext) manager.lookup("org.apache.mailet.MailetContext"));
        setMailServer((MailServer) manager.lookup(MailServer.ROLE));
        setDNSService((DNSService) manager.lookup(DNSService.ROLE));
        setFileSystem((FileSystem) manager.lookup(FileSystem.ROLE));
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration configuration) throws ConfigurationException {
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

        port = configuration.getChild("port").getValueAsInteger(getDefaultPort());

     

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

        
        String connectionLimitString = configuration.getChild("connectionLimit").getValue(null);
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
       
        String connectionLimitPerIP = handlerConfiguration.getChild("connectionLimitPerIP").getValue(null);
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
       
        
        Configuration tlsConfig = configuration.getChild("startTLS");
        if (tlsConfig != null) {
            useStartTLS = tlsConfig.getAttributeAsBoolean("enable", false);

            if (useStartTLS) {
                keystore = tlsConfig.getChild("keystore").getValue(null);
                if (keystore == null) {
                    throw new ConfigurationException("keystore needs to get configured");
                }
                secret = tlsConfig.getChild("secret").getValue("");
            }
        }
        
       helloName = (String) mailetcontext.getAttribute(Constants.HELLO_NAME);
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
    protected Logger getLogger() {
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
    
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        ProtocolCodecFilter codecFactory = new ProtocolCodecFilter(new TextLineCodecFactory());
        Log cLogger = new AvalonLogger(getLogger());

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        builder.addLast("protocolCodecFactory", codecFactory);
        builder.addLast("connectionFilter", new ConnectionFilter(cLogger, connectionLimit, connPerIP));
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
