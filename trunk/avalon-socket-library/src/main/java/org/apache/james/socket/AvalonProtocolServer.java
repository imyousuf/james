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

package org.apache.james.socket;

import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.sockets.ServerSocketFactory;
import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.excalibur.pool.DefaultPool;
import org.apache.avalon.excalibur.pool.HardResourceLimitingPool;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.CommonsLogger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.excalibur.thread.ThreadPool;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.services.FileSystem;
import org.apache.james.socket.api.ProtocolHandlerFactory;
import org.apache.james.socket.api.ProtocolServer;
import org.apache.james.socket.api.Watchdog;

/**
 * Server which creates connection handlers. 
 * The protocol handling will be delegated to a ProtocolHandler
 * create using the ProtocolHandlerFactory.
 */
public class AvalonProtocolServer extends AbstractHandlerFactory
    implements ConnectionHandlerFactory, ObjectFactory, ProtocolServer {

    /**
     * The default value for the connection timeout.
     */
    private static final int DEFAULT_TIMEOUT = 5* 60 * 1000;

    /**
     * The name of the parameter defining the connection timeout.
     */
    private static final String TIMEOUT_NAME = "connectiontimeout";

    /**
     * The default value for the connection backlog.
     */
    private static final int DEFAULT_BACKLOG = 5;

    /**
     * The name of the parameter defining the connection backlog.
     */
    private static final String BACKLOG_NAME = "connectionBacklog";

    /**
     * The name of the parameter defining the service hello name.
     */
    private static final String HELLO_NAME = "helloName";

    /**
     * The ConnectionManager that spawns and manages service connections.
     */
    private JamesConnectionManager connectionManager;

    /**
     * The factory used to generate protocol handlers for this server.
     */
    private ProtocolHandlerFactory protocolHandlerFactory;

    /**
     * The name of the thread group to be used by this service for 
     * generating connections
     */
    private String threadGroup;

    /**
     * The thread pool used by this service that holds the threads
     * that service the client connections.
     */
    private ThreadPool threadPool = null;

    /**
     * The server socket type used to generate connections for this server.
     */
    private String serverSocketType = "plain";

    /**
     * The port on which this service will be made available.
     */
    private int port = -1;

    /**
     * Network interface to which the service will bind.  If not set,
     * the server binds to all available interfaces.
     */
    private InetAddress bindTo = null;

    /**
     * The name of the connection used by this service.  We need to
     * track this so we can tell the ConnectionManager which service
     * to disconnect upon shutdown.
     */
    private String connectionName;

    /**
     * The maximum number of connections allowed for this service.
     */
    private Integer connectionLimit;

    /**
     * The connection idle timeout.  Used primarily to prevent server
     * problems from hanging a connection.
     */
    private int timeout;

    /**
     * The connection backlog.
     */
    private int backlog;

    /**
     * The hello name for the service.
     */
    private String helloName;

    /**
     * Whether this service is enabled.
     */
    private volatile boolean enabled;

    /**
     * Flag holding the disposed state of the component.
     */
    private boolean m_disposed = false;


    /**
     * The pool used to provide Protocol Handler objects
     */
    private Pool theHandlerPool = null;

    /**
     * The factory used to generate Watchdog objects
     */
    private WatchdogFactory theWatchdogFactory = null;
    
    /**
     * The DNSService
     */
    private DNSService dnsService = null;
    
    /**
     * Counts the number of handler instances created.
     * This allows a unique identity to be assigned to each for
     * context sensitive logging.
     */
    private AtomicLong handlerCount = new AtomicLong(0);
    
    private boolean connPerIPConfigured = false;
    private int connPerIP = 0;

    /**
     * If not null, it will be used to dump the tcp commands for debugging purpose
     */
    private String streamDumpDir = null;

    private FileSystem fSystem;
	private SSLSocketFactory factory;

	private String keystore;

	private String secret;    
     
	private boolean useStartTLS;
	
	private HierarchicalConfiguration configuration;

    private ThreadManager threadManager;

    private SocketManager socketManager;

    private Log logger;

    private ServiceManager serviceManager;
	
    /**
     * Gets the DNS Service.
     * @return the dnsServer
     */
    public final DNSService getDnsServer() {
        return dnsService;
    }

    /**
     * Sets the DNS service.
     * @param dnsServer the dnsServer to set
     */
    @Resource(name="org.apache.james.api.dnsservice.DNSService")
    public final void setDNSService(DNSService dnsServer) {
        this.dnsService = dnsServer;
    }

    @Resource(name="org.apache.james.socket.JamesConnectionManager")
    public void setConnectionManager(JamesConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem fSystem) {
    	this.fSystem = fSystem;
    }
    
    protected FileSystem getFileSystem() {
        return fSystem;
    }
    
    @Resource(name="org.apache.commons.logging.Log")
    public void setLog(Log logger) {
        this.logger = logger;
        setupLogger(this, new CommonsLogger(logger, getClass().getName()));
    }
    
    /**
     * Setter for the ProtocolHandlerFactory factory
     * @param protocolHandlerFactory the factory
     */
    @Resource(name="org.apache.james.socket.api.ProtocolHandlerFactory")
    public void setProtocolHandlerFactory(ProtocolHandlerFactory protocolHandlerFactory) {
        this.protocolHandlerFactory = protocolHandlerFactory;
    }
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    @Resource(name="org.apache.avalon.cornerstone.services.threads.ThreadManager")
    public void setThreadManager(ThreadManager threadManager) {
        this.threadManager = threadManager;
    }
    
    @Resource(name="org.apache.avalon.cornerstone.services.sockets.SocketManager")
    public void setSocketManager(SocketManager socketManager) {
        this.socketManager = socketManager;
    }
    
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        super.service( comp );
        serviceManager = comp;
    }

    /**
     * Subclasses should override this method todo any configuration tasks
     * @throws ConfigurationException 
     */
    protected void onConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        
    }

    
    private final void configure() throws ConfigurationException {
        enabled = configuration.getBoolean("[@enabled]", true);
        final Log logger = getLog();
        if (!enabled) {
          logger.info(protocolHandlerFactory.getServiceType() + " disabled by configuration");
          return;
        }

        HierarchicalConfiguration handlerConfiguration = configuration.configurationAt("handler");

        // Send the handler subconfiguration to the super class.  This 
        // ensures that the handler config is passed to the handlers.
        //
        // TODO: This should be rationalized.  The handler element of the
        //       server configuration doesn't really make a whole lot of 
        //       sense.  We should modify the config to get rid of it.
        //       Keeping it for now to maintain backwards compatibility.
        //super.configure(handlerConfiguration);
        
        
        boolean streamdump=handlerConfiguration.getBoolean("streamdump.[@enabled]", false);
        String streamdumpDir=streamdump ? handlerConfiguration.getString("streamdump.@[directory]", null) : null;
        setStreamDumpDir(streamdumpDir);


        port = configuration.getInt("port",protocolHandlerFactory.getDefaultPort());

        String confSocketType = configuration.getString("serverSocketType", null);
        

        if (confSocketType == null) {
            // Only load the useTLS parameter if a specific socket type has not
            // been specified.  This maintains backwards compatibility while
            // allowing us to have more complex (i.e. multiple SSL configuration)
            // deployments
            final boolean useTLS = configuration.getBoolean("useTLS", isDefaultTLSEnabled());
            if (useTLS) {
                serverSocketType = "ssl";
                loadJCEProviders(configuration, logger);
            }
        } else {
            serverSocketType = confSocketType;
        }
     

        StringBuilder infoBuffer;
        threadGroup = configuration.getString("threadGroup",null);
        if (threadGroup != null) {
            infoBuffer =
                new StringBuilder(64)
                        .append(protocolHandlerFactory.getServiceType())
                        .append(" uses thread group: ")
                        .append(threadGroup);
            logger.info(infoBuffer.toString());
        }
        else {
            logger.info(protocolHandlerFactory.getServiceType() + " uses default thread group.");
        }

        try {
            final String bindAddress = configuration.getString("bind",null);
            if( null != bindAddress ) {
                bindTo = InetAddress.getByName(bindAddress);
                infoBuffer =
                    new StringBuilder(64)
                            .append(protocolHandlerFactory.getServiceType())
                            .append(" bound to: ")
                            .append(bindTo);
                logger.info(infoBuffer.toString());
            }
        }
        catch( final UnknownHostException unhe ) {
            throw new ConfigurationException( "Malformed bind parameter in configuration of service " + protocolHandlerFactory.getServiceType(), unhe );
        }

        configureHelloName(handlerConfiguration);

        timeout = handlerConfiguration.getInteger(TIMEOUT_NAME, DEFAULT_TIMEOUT);

        infoBuffer =
            new StringBuilder(64)
                    .append(protocolHandlerFactory.getServiceType())
                    .append(" handler connection timeout is: ")
                    .append(timeout);
        logger.info(infoBuffer.toString());

        backlog = configuration.getInteger(BACKLOG_NAME, DEFAULT_BACKLOG);

        infoBuffer =
                    new StringBuilder(64)
                    .append(protocolHandlerFactory.getServiceType())
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
            if (connectionLimit.intValue() < 0) {
                logger.error("Connection limit value cannot be less than zero.");
                throw new ConfigurationException("Connection limit value cannot be less than zero.");
            }
        } else {
            connectionLimit = new Integer(connectionManager.getMaximumNumberOfOpenConnections());
        }
        infoBuffer = new StringBuilder(128)
            .append(protocolHandlerFactory.getServiceType())
            .append(" will allow a maximum of ")
            .append(connectionLimit.intValue())
            .append(" connections.");
        logger.info(infoBuffer.toString());
        
        String connectionLimitPerIP = configuration.getString("connectionLimitPerIP", null);
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
            .append(protocolHandlerFactory.getServiceType())
            .append(" will allow a maximum of ")
            .append(connPerIP)
            .append(" per IP connections for " +protocolHandlerFactory.getServiceType());
        logger.info(infoBuffer.toString());
        
       		useStartTLS = configuration.getBoolean("startTLS.[@enable]", false);
       		
       		if (useStartTLS) {
       			keystore = configuration.getString("startTLS.keystore", null);
       			if (keystore == null) {
       				throw new ConfigurationException("keystore needs to get configured");
       			}
       			secret = configuration.getString("startTLS.secret","");
				loadJCEProviders(configuration.configurationAt("startTLS"), getLog());
       		}
       	onConfigure(configuration);
    }

    protected Log getLog() {
        return logger;
    }
    
    @SuppressWarnings("unchecked")
    private void loadJCEProviders(Configuration conf, final Log logger) throws ConfigurationException {
        final List<String> providerConfiguration = conf.getList("provider");
        for (int i = 0; i < providerConfiguration.size(); i++) {
            final String providerName = providerConfiguration.get(i);
            loadProvider(logger, providerName);
        }
    }

    private void loadProvider(final Log logger, final String providerName) {
        if (providerName == null) {
            logger.warn("Failed to specify provider. Continuing but JCE provider will not be loaded");   
        } else {
            try {
                logger.debug("Trying to load JCE provider '" + providerName + "'");
                Security.addProvider((Provider) Class.forName(providerName).newInstance());
                logger.info("Load JCE provider '" + providerName + "'");
            } catch (IllegalAccessException e) {
                logJCELoadFailure(logger, providerName, e);
            } catch (InstantiationException e) {
                logJCELoadFailure(logger, providerName, e);
            } catch (ClassNotFoundException e) {
                logJCELoadFailure(logger, providerName, e);
            } catch (RuntimeException e) {
                logJCELoadFailure(logger, providerName, e);
            }
        }
    }

    private void logJCELoadFailure(final Log logger, final String providerName, Exception e) {
        logger.warn("Cannot load JCE provider" + providerName);
        logger.debug(e.getMessage(), e);
    }

    private void setStreamDumpDir(String streamdumpDir) {
        this.streamDumpDir = streamdumpDir;
    }
    
    private void configureHelloName(HierarchicalConfiguration handlerConfiguration) {
        StringBuilder infoBuffer;
        String hostName = null;
        try {
            hostName = dnsService.getHostName(dnsService.getLocalHost());
        } catch (UnknownHostException ue) {
            hostName = "localhost";
        }

        infoBuffer =
            new StringBuilder(64)
                    .append(protocolHandlerFactory.getServiceType())
                    .append(" is running on: ")
                    .append(hostName);
        getLog().info(infoBuffer.toString());

        
 
        if (handlerConfiguration.getKeys(HELLO_NAME).hasNext()) {
            boolean autodetect = handlerConfiguration.getBoolean(HELLO_NAME +".[@autodetect]", true);
            if (autodetect) {
                helloName = hostName;
            } else {
                // Should we use the defaultdomain here ?
                helloName = handlerConfiguration.getString(HELLO_NAME, "localhost");
            }
        } else {
            helloName = null;
        }
        infoBuffer =
            new StringBuilder(64)
                    .append(protocolHandlerFactory.getServiceType())
                    .append(" handler hello name is: ")
                    .append(helloName);
        getLog().info(infoBuffer.toString());
    }


    @PostConstruct
    public void init() throws Exception {
        
        getLog().debug(protocolHandlerFactory.getServiceType() + " init...");

        // parse configuration
        configure();

        if (!isEnabled()) {
            getLog().info(protocolHandlerFactory.getServiceType() + " Disabled");
            return;
        }      
        
        protocolHandlerFactory.prepare(this);

        if (useStartTLS) {
        	initStartTLS();
        }
       
        initializeThreadPool(threadManager);

        initializeServerSocket(socketManager);

        getLog().debug(protocolHandlerFactory.getServiceType() + " ...init end");

        initializeHandlerPool();
        
        // do avalon specific preparations
        ContainerUtil.enableLogging(theHandlerPool, getLogger());
        ContainerUtil.initialize(theHandlerPool);

        theWatchdogFactory = getWatchdogFactory();

        // Allow subclasses to perform initialisation
        protocolHandlerFactory.doInit();
    }
    
    private void initStartTLS() throws Exception {
    	KeyStore ks = null;
		KeyManagerFactory kmf = null;
		SSLContext sslcontext = null;

		// This loads the key material, and initialises the
		// SSLSocketFactory
		// This should be done once!!
		// Note: in order to load SunJCE provider the jre/lib/ext should be
		// added
		// to the java.ext.dirs see the note in run.sh script
		try {
			// just to see SunJCE is loaded
			Provider[] provs = Security.getProviders();
			for (int i = 0; i < provs.length; i++)
				getLog().debug("Provider[" + i + "]=" + provs[i].getName());

			char[] passphrase = secret.toCharArray();
			ks = KeyStore.getInstance("JKS","SUN");
			ks.load(fSystem.getResource(keystore), passphrase);
			kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
			kmf.init(ks, passphrase);
			sslcontext = SSLContext.getInstance("SSL", "SunJSSE");
			sslcontext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			getLog().error("Exception accessing keystore: " + e);
			throw e;
		}
		factory = sslcontext.getSocketFactory();
		// just to see the list of supported ciphers
		String[] ss = factory.getSupportedCipherSuites();
		getLog().debug("list of supported ciphers");
		for (int i = 0; i < ss.length; i++)
			getLog().debug(ss[i]);
    }

    private void initializeThreadPool(ThreadManager threadManager) {
        if (threadGroup != null) {
            threadPool = threadManager.getThreadPool(threadGroup);
        } else {
            threadPool = threadManager.getDefaultThreadPool();
        }
    }

    private void initializeServerSocket(SocketManager socketManager) throws Exception {
        try {
            initializeServerSocketWorker(socketManager);
        } catch (BindException e) {
            // handle a common exception and give detailed error message
            String errorMessage = getBindingErrorMessage(e);
            System.out.println("------------------------------");
            System.out.println(errorMessage);
            System.out.println("------------------------------");
            getLog().error(errorMessage);
            throw e;
        }       
    }
     
    private String getBindingErrorMessage(BindException e) {
        // general info about binding error
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("FATAL ERROR when starting service '").append(protocolHandlerFactory.getServiceType()).append("'! ");
        errorMessage.append("could not bind to ");
        errorMessage.append(bindTo == null ? "0.0.0.0" : bindTo.toString());
        errorMessage.append(":").append(port).append(". ");
        
        // try to deliver more specific information 
        if (e.getMessage().indexOf("Address already in use") != -1) {
            errorMessage.append("Port is already exclusively in use by another application.");
        } else if (e.getMessage().indexOf("Permission denied") != -1) {
            errorMessage.append("The user account James is running under has not enough privileges to bind to this ");
            if (port < 1024) errorMessage.append("privileged ");
            errorMessage.append("port.");
        } else {
            errorMessage.append(e.getMessage());
        }
        return errorMessage.toString();
    }

    private void initializeServerSocketWorker(SocketManager socketManager) throws Exception {
        ServerSocketFactory factory = socketManager.getServerSocketFactory(serverSocketType);
        ServerSocket serverSocket = factory.createServerSocket(port, backlog, bindTo);

        if (null == connectionName) {
            final StringBuilder sb = new StringBuilder();
            sb.append(serverSocketType);
            sb.append(':');
            sb.append(port);

            if (null != bindTo) {
                sb.append('/');
                sb.append(bindTo);
            }
            connectionName = sb.toString();
        }

        if ((connectionLimit != null)) {
            if (null != threadPool) {
            if (connPerIPConfigured) {
                    connectionManager.connect(connectionName, serverSocket, this, threadPool, connectionLimit.intValue(),connPerIP);
            } else {
                connectionManager.connect(connectionName, serverSocket, this, threadPool, connectionLimit.intValue());
            }
            } else {
            if (connPerIPConfigured) {
                    connectionManager.connect(connectionName, serverSocket, this, connectionLimit.intValue(),connPerIP); // default pool
                } else {
                    connectionManager.connect(connectionName, serverSocket, this, connectionLimit.intValue());
                }
            }
        } else {
            if (null != threadPool) {
            if (connPerIPConfigured) {
                    connectionManager.connect(connectionName, serverSocket, this, threadPool);
            } else {
                connectionManager.connect(connectionName, serverSocket, this, threadPool, 0, connPerIP);
            }
            } else {
            if (connPerIPConfigured) {
                    connectionManager.connect(connectionName, serverSocket, this); // default pool
            } else {
                    connectionManager.connect(connectionName, serverSocket, this, 0, connPerIP);
            }
            }
        }
    }

    private void initializeHandlerPool() throws Exception {
        StringBuilder logBuffer =
                new StringBuilder(64)
                        .append(protocolHandlerFactory.getServiceType())
                        .append(" started ")
                        .append(connectionName);
        String logString = logBuffer.toString();
        System.out.println(logString);
        getLog().info(logString);

        if (connectionLimit != null) {
            theHandlerPool = new HardResourceLimitingPool(this, 5, connectionLimit.intValue());
            if (getLog().isDebugEnabled()) {
                getLog().debug("Using a bounded pool for "+protocolHandlerFactory.getServiceType()+" handlers with upper limit " + connectionLimit.intValue());
            }
        } else {
            // NOTE: The maximum here is not a real maximum.  The handler pool will continue to
            //       provide handlers beyond this value.
            theHandlerPool = new DefaultPool(this, null, 5, 30);
            getLog().debug("Using an unbounded pool for "+protocolHandlerFactory.getServiceType()+" handlers.");
        }
    }
    
    @PreDestroy
    public void dispose() {

        if (!isEnabled()) {
            return;
        }

        if( m_disposed )
        {
            if( getLog().isWarnEnabled() )
            {
                getLog().warn( "ignoring disposal request - already disposed" );
            }
            return;
        }

        if( getLog().isDebugEnabled() )
        {
            getLog().debug( "disposal" );
        }

        m_disposed = true;
        if( getLog().isDebugEnabled() )
        {
            StringBuilder infoBuffer =
               new StringBuilder(64).append(protocolHandlerFactory.getServiceType()).append(
                   " dispose... ").append(connectionName);
            getLog().debug(infoBuffer.toString());
        }

        try {
            connectionManager.disconnect(connectionName, true);
        } catch (final Exception e) {
            StringBuilder warnBuffer =
                new StringBuilder(64)
                        .append("Error disconnecting ")
                        .append(protocolHandlerFactory.getServiceType())
                        .append(": ");
            getLog().warn(warnBuffer.toString(), e);
        }

        connectionManager = null;
        threadPool = null;

        // This is needed to make sure sockets are promptly closed on Windows 2000
        // TODO: Check this - shouldn't need to explicitly gc to force socket closure
        System.gc();

        getLog().debug(protocolHandlerFactory.getServiceType() + " ...dispose end");
    }

    /**
     * This constructs the WatchdogFactory that will be used to guard
     * against runaway or stuck behavior.  Should only be called once
     * by a subclass in its initialize() method.
     *
     * @return the WatchdogFactory to be employed by subclasses.
     */
    private WatchdogFactory getWatchdogFactory() {
        WatchdogFactory theWatchdogFactory = null;
        theWatchdogFactory = new ThreadPerWatchdogFactory(threadPool, timeout);
        return theWatchdogFactory;
     }


    /**
     * Describes whether this service is enabled by configuration.
     *
     * @return is the service enabled.
     */
    public final boolean isEnabled() {
        return enabled;
    }
    /**
     * Override this method to create actual instance of connection handler.
     *
     * @return the new ConnectionHandler
     * @exception Exception if an error occurs
     */
    protected ConnectionHandler newHandler()
            throws Exception {
        JamesConnectionBridge theHandler = (JamesConnectionBridge)theHandlerPool.get();
        
        if (getLog().isDebugEnabled()) {
            getLog().debug("Handler [" +  theHandler + "] obtained from pool.");
        }

        Watchdog theWatchdog = theWatchdogFactory.getWatchdog(theHandler);
        ContainerUtil.enableLogging(theWatchdog,getLogger());
        theHandler.setStreamDumpDir(streamDumpDir);
        theHandler.setWatchdog(theWatchdog);
        return theHandler;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory#releaseConnectionHandler(ConnectionHandler)
     */
    public void releaseConnectionHandler( ConnectionHandler connectionHandler ) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Returning Handler [" +  connectionHandler + "] to pool.");
        }
        theHandlerPool.put((Poolable)connectionHandler);
    }

    /**
     * Get whether TLS is enabled for this server's socket by default.
     *
     * @return the default port
     */
     protected boolean isDefaultTLSEnabled() {
        return false;
     }
    
    /**
    * Returns the port that the service is bound to 
    * 
    * @return int The port number     
    */  
    public int  getPort() {
        return port;
    }
    
    /**
    * Returns the address if the network interface the socket is bound to 
    * 
    * @return String The network interface name     
    */  
    public String  getNetworkInterface() {
        if (bindTo == null) {
            return "All";
        } else {
            return bindTo.getHostAddress();
        }
    }
    
    /**
    * Returns the server socket type, plain or SSL 
    * 
    * @return String The socket type, plain or SSL     
    */  
    public String  getSocketType() {
        return serverSocketType;
    }
    
    /**
    * @see org.apache.avalon.excalibur.pool.ObjectFactory#decommission(Object)
    */
    public void decommission( Object object ) throws Exception {
        return;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#createConnectionHandler()
     */
    
    public ConnectionHandler createConnectionHandler() throws Exception {
        ConnectionHandler conn = super.createConnectionHandler();
        ContainerUtil.service(conn, serviceManager);
        return conn;
    }
   
    
    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public Object newInstance() throws Exception {
        final String serviceShortNameString;
        final String serviceType = protocolHandlerFactory.getServiceType();
        final int firstSpace = serviceType.indexOf(' ');
        if (firstSpace > 0) {
            serviceShortNameString = serviceType.substring(0, firstSpace);
        } else {
            serviceShortNameString = serviceType;
        }
        final String name = serviceShortNameString + "Handler-" + handlerCount.getAndAdd(1);
        final JamesConnectionBridge delegatingJamesHandler;
        
        if (useStartTLS) {
        	delegatingJamesHandler = new JamesConnectionBridge(protocolHandlerFactory.newProtocolHandlerInstance(), dnsService, name, getLog(), factory);
        } else {
            delegatingJamesHandler = new JamesConnectionBridge(protocolHandlerFactory.newProtocolHandlerInstance(), dnsService, name, getLog());
        }
        return delegatingJamesHandler;
        
    }

    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
     */
    @SuppressWarnings("unchecked")
    public Class getCreatedClass() {
        return JamesConnectionBridge.class;
    }


    public boolean useStartTLS() {
    	return useStartTLS;
    }
    
    public String getHelloName() {
        return helloName;
    }

}

