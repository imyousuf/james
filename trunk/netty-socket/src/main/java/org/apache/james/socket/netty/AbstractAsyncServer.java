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
package org.apache.james.socket.netty;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Abstract base class for Servers which want to use async io
 *
 */
public abstract class AbstractAsyncServer implements LogEnabled, Configurable{
    /**
     * The default value for the connection backlog.
     */
    private static final int DEFAULT_BACKLOG = 200;
    
    /**
     * The default value for the connection timeout.
     */
    private static final int DEFAULT_TIMEOUT = 5* 60;

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
    
    private FileSystem fileSystem;
    
    /**
     * The internal mail server service.
     */
    private MailServer mailServer;

    private Log logger;

    private DNSService dns;

    private boolean enabled;

    protected int connPerIP;

    private boolean useStartTLS;
    private boolean useSSL;


    protected int connectionLimit;

    private String helloName;
    
    private String keystore;

    private String secret;
    
    private int backlog;
    
    private InetAddress bindTo;

    private int port;

    private int timeout;

    private SSLContext context;

    private ServerBootstrap bootstrap;  

    @Resource(name="dnsserver")
    public final void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    
    @Resource(name="filesystem")
    public final void setFileSystem(FileSystem filesystem) {
        this.fileSystem = filesystem;
    }
    
    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public final void setLog(Log logger) {
       this.logger = logger;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public final void configure(HierarchicalConfiguration config) throws ConfigurationException{
        
        Configuration handlerConfiguration = ((HierarchicalConfiguration)config).configurationAt("handler");

        enabled = config.getBoolean("[@enabled]", true);
        
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

        port = config.getInt("port",getDefaultPort());

     

        StringBuilder infoBuffer;
        

        try {
            final String bindAddress = config.getString("bind",null);
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

        backlog = config.getInt(BACKLOG_NAME,DEFAULT_BACKLOG);

        infoBuffer =
                    new StringBuilder(64)
                    .append(getServiceType())
                    .append(" connection backlog is: ")
                    .append(backlog);
        logger.info(infoBuffer.toString());

        
        String connectionLimitString = config.getString("connectionLimit",null);
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
       

        useStartTLS = config.getBoolean("tls.[@startTLS]", false);
        useSSL = config.getBoolean("tls.[@socketTLS]", false);

        if (useSSL && useStartTLS) throw new ConfigurationException("startTLS is only supported when using plain sockets");
       
        if (useStartTLS || useSSL) {
            keystore = config.getString("tls.keystore", null);
            if (keystore == null) {
                throw new ConfigurationException("keystore needs to get configured");
            }
            secret = config.getString("tls.secret","");
        }
             
        doConfigure(config);

    }
    
    
    @PostConstruct
    public final void init() throws Exception {
        if (isEnabled()) {
            preInit();
            buildSSLContext();

            bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                                                   Executors.newCachedThreadPool(),
                                           Executors.newCachedThreadPool()));
            // Configure the pipeline factory.
            bootstrap.setPipelineFactory(createPipelineFactory());
         
            // Bind and start to accept incoming connections.
            bootstrap.setOption("backlog",backlog);
            bootstrap.setOption("reuseAddress",true);

            //acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, timeout );
            //acceptor.setHandler(createIoHandler());
            bootstrap.bind(new InetSocketAddress(bindTo,port));
        }
    }

    @PreDestroy
    public final void destroy() {
        getLogger().info("Dispose " + getServiceType());
        
        bootstrap.releaseExternalResources();
    }
    
    
    /**
     * This method is called on init of the Server. Subclasses should override this method to init stuff
     *
     * @throws Exception 
     */
    protected void preInit() throws Exception {
        // override me
    }
    
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException {
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
     * Return the FileSystem
     * 
     * @return fileSystem
     */
    protected FileSystem getFileSystem() {
        return fileSystem;
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

        boolean autodetect = handlerConfiguration.getBoolean(HELLO_NAME + "/[@autodetect]", true);
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
     * Return if the socket is using SSL
     * 
     * @return useSSL
     */
    protected boolean isSSLSocket() {
        return useSSL;
    }
    
    /**
     * Build the SSLEngine
     * 
     * @throws Exception
     */
    
    private void buildSSLContext() throws Exception {
        if (useStartTLS) {
            String algorithm = "SunX509";
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(fileSystem.getFile(keystore)), secret.toCharArray());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, secret.toCharArray());

            // Initialize the SSLContext to work with our key managers.
            context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            

        }
    }
   
    
    
    /**
     * Createh IoHandler to use by this Server implementation
     * 
     * @return ioHandler
     */
    protected abstract ChannelPipelineFactory createPipelineFactory();
    
    /**
     * Return the SslContextFactory which was created for this service. 
     * 
     * @return contextFactory
     */
    
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
    
    protected int getTimeout() {
        return timeout;
    }
    
    protected SSLContext getSSLContext() {
        return context;
    }
}
