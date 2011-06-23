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
package org.apache.james.protocols.lib.netty;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;

import org.apache.james.protocols.impl.AbstractAsyncServer;
import org.apache.james.protocols.lib.jmx.ServerMBean;
import org.apache.james.util.concurrent.JMXEnabledThreadPoolExecutor;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;

/**
 * Abstract base class for Servers for all James Servers
 */
public abstract class AbstractConfigurableAsyncServer extends AbstractAsyncServer implements LogEnabled, Configurable, ServerMBean {
    /** The default value for the connection backlog. */
    private static final int DEFAULT_BACKLOG = 200;

    /** The default value for the connection timeout. */
    private static final int DEFAULT_TIMEOUT = 5 * 60;

    /** The name of the parameter defining the connection timeout. */
    private static final String TIMEOUT_NAME = "connectiontimeout";

    /** The name of the parameter defining the connection backlog. */
    private static final String BACKLOG_NAME = "connectionBacklog";

    /** The name of the parameter defining the service hello name. */
    public static final String HELLO_NAME = "helloName";

    // By default, use the Sun X509 algorithm that comes with the Sun JCE
    // provider for SSL
    // certificates
    private static final String defaultX509algorithm = "SunX509";

    // The X.509 certificate algorithm
    private String x509Algorithm = defaultX509algorithm;

    private FileSystem fileSystem;

    private Logger logger;

    private boolean enabled;

    protected int connPerIP;

    private boolean useStartTLS;
    private boolean useSSL;

    protected int connectionLimit;

    private String helloName;

    private String keystore;

    private String secret;

    private SSLContext context;

    protected String jmxName;

    private String[] enabledCipherSuites;

    private ConnectionCountHandler countHandler = new ConnectionCountHandler();

    private ExecutionHandler executionHandler = null;

    private int maxExecutorThreads;

    @Resource(name = "filesystem")
    public final void setFileSystem(FileSystem filesystem) {
        this.fileSystem = filesystem;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.slf4j.Logger)
     */
    public final void setLog(Logger logger) {
        this.logger = logger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.lifecycle.Configurable#configure(org.apache.commons.
     * configuration.HierarchicalConfiguration)
     */
    public final void configure(HierarchicalConfiguration config) throws ConfigurationException {

        enabled = config.getBoolean("[@enabled]", true);

        final Logger logger = getLogger();
        if (!enabled) {
            logger.info(getServiceType() + " disabled by configuration");
            return;
        }

        String listen[] = config.getString("bind", "0.0.0.0:" + getDefaultPort()).split(",");
        List<InetSocketAddress> bindAddresses = new ArrayList<InetSocketAddress>();
        for (int i = 0; i < listen.length; i++) {
            String bind[] = listen[i].split(":");

            InetSocketAddress address;
            String ip = bind[0].trim();
            int port = Integer.parseInt(bind[1].trim());
            if (ip.equals("0.0.0.0") == false) {
                try {
                    ip = InetAddress.getByName(ip).getHostName();
                } catch (final UnknownHostException unhe) {
                    throw new ConfigurationException("Malformed bind parameter in configuration of service " + getServiceType(), unhe);
                }
            }
            address = new InetSocketAddress(ip, port);

            StringBuilder infoBuffer = new StringBuilder(64).append(getServiceType()).append(" bound to: ").append(ip).append(":").append(port);
            logger.info(infoBuffer.toString());

            bindAddresses.add(address);
        }
        setListenAddresses(bindAddresses);

        jmxName = config.getString("jmxName", getDefaultJMXName());
        int ioWorker = config.getInt("ioWorkerCount", DEFAULT_IO_WORKER_COUNT);
        setIoWorkerCount(ioWorker);

        maxExecutorThreads = config.getInt("maxExecutorCount", 50);

        
        configureHelloName(config);

        setTimeout(config.getInt(TIMEOUT_NAME, DEFAULT_TIMEOUT));

        StringBuilder infoBuffer = new StringBuilder(64).append(getServiceType()).append(" handler connection timeout is: ").append(getTimeout());
        logger.info(infoBuffer.toString());

        setBacklog(config.getInt(BACKLOG_NAME, DEFAULT_BACKLOG));

        infoBuffer = new StringBuilder(64).append(getServiceType()).append(" connection backlog is: ").append(getBacklog());
        logger.info(infoBuffer.toString());

        String connectionLimitString = config.getString("connectionLimit", null);
        if (connectionLimitString != null) {
            try {
                connectionLimit = new Integer(connectionLimitString);
            } catch (NumberFormatException nfe) {
                logger.error("Connection limit value is not properly formatted.", nfe);
            }
            if (connectionLimit < 0) {
                logger.error("Connection limit value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            } else if (connectionLimit > 0) {
                infoBuffer = new StringBuilder(128).append(getServiceType()).append(" will allow a maximum of ").append(connectionLimitString).append(" connections.");
                logger.info(infoBuffer.toString());
            }
        }

        String connectionLimitPerIP = config.getString("connectionLimitPerIP", null);
        if (connectionLimitPerIP != null) {
            try {
                connPerIP = new Integer(connectionLimitPerIP).intValue();
            } catch (NumberFormatException nfe) {
                logger.error("Connection limit per IP value is not properly formatted.", nfe);
            }
            if (connPerIP < 0) {
                logger.error("Connection limit per IP value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            } else if (connPerIP > 0) {
                infoBuffer = new StringBuilder(128).append(getServiceType()).append(" will allow a maximum of ").append(connPerIP).append(" per IP connections for " + getServiceType());
                logger.info(infoBuffer.toString());
            }
        }

        useStartTLS = config.getBoolean("tls.[@startTLS]", false);
        useSSL = config.getBoolean("tls.[@socketTLS]", false);

        if (useSSL && useStartTLS)
            throw new ConfigurationException("startTLS is only supported when using plain sockets");

        if (useStartTLS || useSSL) {
            enabledCipherSuites = config.getStringArray("tls.supportedCipherSuites.cipherSuite");
            keystore = config.getString("tls.keystore", null);
            if (keystore == null) {
                throw new ConfigurationException("keystore needs to get configured");
            }
            secret = config.getString("tls.secret", "");
            x509Algorithm = config.getString("tls.algorithm", defaultX509algorithm);
        }

        doConfigure(config);

    }

    @PostConstruct
    public final void init() throws Exception {
        if (isEnabled()) {
            preInit();
            buildSSLContext();
            executionHandler = createExecutionHander();
            bind();
            
            getLogger().info("Init " + getServiceType() + " done");

        }
    }

    @PreDestroy
    public final void destroy() {
        getLogger().info("Dispose " + getServiceType());
        if (isEnabled()) {
            unbind();
            postDestroy();

            if (executionHandler != null) {
                executionHandler.releaseExternalResources();
            }
            
        }
        getLogger().info("Dispose " + getServiceType() + " done");

    }

    protected void postDestroy() {
        
    }
    
    
    /**
     * This method is called on init of the Server. Subclasses should override
     * this method to init stuff
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
     * @throws ConfigurationException
     */
    protected void configureHelloName(Configuration handlerConfiguration) throws ConfigurationException {
        StringBuilder infoBuffer;
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ue) {
            hostName = "localhost";
        }

        infoBuffer = new StringBuilder(64).append(getServiceType()).append(" is running on: ").append(hostName);
        getLogger().info(infoBuffer.toString());

        boolean autodetect = handlerConfiguration.getBoolean(HELLO_NAME + ".[@autodetect]", true);
        if (autodetect) {
            helloName = hostName;
        } else {
            helloName = handlerConfiguration.getString(HELLO_NAME);
            if (helloName == null || helloName.trim().length() < 1) {
                throw new ConfigurationException("Please configure the helloName or use autodetect");
            }
        }

        infoBuffer = new StringBuilder(64).append(getServiceType()).append(" handler hello name is: ").append(helloName);
        getLogger().info(infoBuffer.toString());
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
        if (useStartTLS || useSSL) {
            FileInputStream fis = null;
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                fis = new FileInputStream(fileSystem.getFile(keystore));
                ks.load(fis, secret.toCharArray());

                // Set up key manager factory to use our key store
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(x509Algorithm);
                kmf.init(ks, secret.toCharArray());

                // Initialize the SSLContext to work with our key managers.
                context = SSLContext.getInstance("TLS");
                context.init(kmf.getKeyManagers(), null, null);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }

    /**
     * Return the default port which will get used for this server if non is
     * specify in the configuration
     * 
     * @return port
     */
    protected abstract int getDefaultPort();

    /**
     * Return the SSLContext to use
     * 
     * @return sslContext
     */
    protected SSLContext getSSLContext() {
        return context;
    }

    /**
     * Return the socket type. The Socket type can be secure or plain
     * 
     * @return
     */
    public String getSocketType() {
        if (isSSLSocket()) {
            return "secure";
        }
        return "plain";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#getStartTLSSupported()
     */
    public boolean getStartTLSSupported() {
        return isStartTLSSupported();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.socket.ServerMBean#getMaximumConcurrentConnections()
     */
    public int getMaximumConcurrentConnections() {
        return connectionLimit;
    }

    @Override
    protected Executor createBossExecutor() {
        return JMXEnabledThreadPoolExecutor.newCachedThreadPool("org.apache.james:type=server,name=" + jmxName + ",sub-type=threadpool", "boss");
    }

    @Override
    protected Executor createWorkerExecutor() {
        return JMXEnabledThreadPoolExecutor.newCachedThreadPool("org.apache.james:type=server,name=" + jmxName + ",sub-type=threadpool", "worker");
    }

    /**
     * Return the default name of the the server in JMX if none is configured
     * via "jmxname" in the configuration
     * 
     * @return defaultJmxName
     */
    protected abstract String getDefaultJMXName();

    protected String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#isStarted()
     */
    public boolean isStarted() {
        return isBound();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#start()
     */
    public boolean start() {
        try {
            bind();
        } catch (Exception e) {
            logger.error("Unable to start server");
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#stop()
     */
    public boolean stop() {
        unbind();
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.server.jmx.ServerMBean#getHandledConnections()
     */
    public long getHandledConnections() {
        return countHandler.getConnectionsTillStartup();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#getCurrentConnections()
     */
    public int getCurrentConnections() {
        return countHandler.getCurrentConnectionCount();
    }

    protected ConnectionCountHandler getConnectionCountHandler() {
        return countHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.protocols.lib.jmx.ServerMBean#getBoundAddresses()
     */
    public String[] getBoundAddresses() {

        List<InetSocketAddress> addresses = getListenAddresses();
        String[] addrs = new String[addresses.size()];
        for (int i = 0; i < addresses.size(); i++) {
            InetSocketAddress address = addresses.get(i);
            addrs[i] = address.getHostName() + ":" + address.getPort();
        }

        return addrs;
    }

    @Override
    protected void configureBootstrap(ServerBootstrap bootstrap) {
        super.configureBootstrap(bootstrap);
        
        // enable tcp keep-alives
        bootstrap.setOption("child.keepAlive", true);
    }
    
    /**
     * Create a new {@link ExecutionHandler} which is used to execute IO-Bound handlers
     * 
     * @return ehandler
     */
    protected ExecutionHandler createExecutionHander() {
        return new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(maxExecutorThreads, 0, 0));
    }

    /**
     * Return the {@link ExecutionHandler} or null if non should be used. Be sure you call {@link #createExecutionHander()} before
     * 
     * @return ehandler
     */
    protected ExecutionHandler getExecutionHandler() {
        return executionHandler;
    }
    
    protected abstract OneToOneEncoder createEncoder();

    protected abstract ChannelUpstreamHandler createCoreHandler();
    
    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new AbstractExecutorAwareChannelPipelineFactory(connPerIP, connPerIP, connPerIP, group, enabledCipherSuites) {
            @Override
            protected SSLContext getSSLContext() {
                return AbstractConfigurableAsyncServer.this.getSSLContext();

            }

            @Override
            protected boolean isSSLSocket() {
                return AbstractConfigurableAsyncServer.this.isSSLSocket();
            }

            @Override
            protected OneToOneEncoder createEncoder() {
                return AbstractConfigurableAsyncServer.this.createEncoder();

            }

            @Override
            protected ChannelUpstreamHandler createHandler() {
                return AbstractConfigurableAsyncServer.this.createCoreHandler();

            }

            @Override
            protected ConnectionCountHandler getConnectionCountHandler() {
                return AbstractConfigurableAsyncServer.this.getConnectionCountHandler();
            }

            @Override
            protected ExecutionHandler getExecutionHandler() {
                return AbstractConfigurableAsyncServer.this.getExecutionHandler();
            }

        };
    }
    
    
}
