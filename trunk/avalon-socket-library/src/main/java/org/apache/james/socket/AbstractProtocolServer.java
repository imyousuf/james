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
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

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
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.excalibur.thread.ThreadPool;
import org.apache.james.api.dnsservice.DNSService;

/**
 * Server which creates connection handlers. All new James service must
 * inherit from this abstract implementation.
 *
 */
public abstract class AbstractProtocolServer extends AbstractHandlerFactory
    implements Serviceable, Configurable, Disposable, ConnectionHandlerFactory, ObjectFactory {

    /**
     * The default value for the connection timeout.
     */
    protected static final int DEFAULT_TIMEOUT = 5* 60 * 1000;

    /**
     * The name of the parameter defining the connection timeout.
     */
    protected static final String TIMEOUT_NAME = "connectiontimeout";

    /**
     * The default value for the connection backlog.
     */
    protected static final int DEFAULT_BACKLOG = 5;

    /**
     * The name of the parameter defining the connection backlog.
     */
    protected static final String BACKLOG_NAME = "connectionBacklog";

    /**
     * The name of the parameter defining the service hello name.
     */
    public static final String HELLO_NAME = "helloName";

    /**
     * The ConnectionManager that spawns and manages service connections.
     */
    private JamesConnectionManager connectionManager;

    /**
     * The name of the thread group to be used by this service for 
     * generating connections
     */
    protected String threadGroup;

    /**
     * The thread pool used by this service that holds the threads
     * that service the client connections.
     */
    protected ThreadPool threadPool = null;

    /**
     * The server socket type used to generate connections for this server.
     */
    protected String serverSocketType = "plain";

    /**
     * The port on which this service will be made available.
     */
    protected int port = -1;

    /**
     * Network interface to which the service will bind.  If not set,
     * the server binds to all available interfaces.
     */
    protected InetAddress bindTo = null;

    /**
     * The server socket associated with this service
     */
    protected ServerSocket serverSocket;

    /**
     * The name of the connection used by this service.  We need to
     * track this so we can tell the ConnectionManager which service
     * to disconnect upon shutdown.
     */
    protected String connectionName;

    /**
     * The maximum number of connections allowed for this service.
     */
    protected Integer connectionLimit;

    /**
     * The connection idle timeout.  Used primarily to prevent server
     * problems from hanging a connection.
     */
    protected int timeout;

    /**
     * The connection backlog.
     */
    protected int backlog;

    /**
     * The hello name for the service.
     */
    protected String helloName;

    /**
     * The component manager used by this service.
     */
    private ServiceManager componentManager;

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
    protected Pool theHandlerPool = null;

    /**
     * The factory used to generate Watchdog objects
     */
    protected WatchdogFactory theWatchdogFactory = null;
    
    /**
     * The DNSService
     */
    private DNSService dnsServer = null;
    
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

    public void setConnectionManager(JamesConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        super.service( comp );
        componentManager = comp;
        JamesConnectionManager connectionManager =
            (JamesConnectionManager)componentManager.lookup(JamesConnectionManager.ROLE);
        setConnectionManager(connectionManager);
        dnsServer = (DNSService) comp.lookup(DNSService.ROLE);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        enabled = conf.getAttributeAsBoolean("enabled", true);
        final Logger logger = getLogger();
        if (!enabled) {
          logger.info(getServiceType() + " disabled by configuration");
          return;
        }

        Configuration handlerConfiguration = conf.getChild("handler");

        // Send the handler subconfiguration to the super class.  This 
        // ensures that the handler config is passed to the handlers.
        //
        // TODO: This should be rationalized.  The handler element of the
        //       server configuration doesn't really make a whole lot of 
        //       sense.  We should modify the config to get rid of it.
        //       Keeping it for now to maintain backwards compatibility.
        super.configure(handlerConfiguration);
        
        
        boolean streamdump=handlerConfiguration.getChild("streamdump").getAttributeAsBoolean("enabled", false);
        String streamdumpDir=streamdump ? handlerConfiguration.getChild("streamdump").getAttribute("directory", null) : null;
        setStreamDumpDir(streamdumpDir);


        port = conf.getChild("port").getValueAsInteger(getDefaultPort());

        Configuration serverSocketTypeConf = conf.getChild("serverSocketType", false);
        String confSocketType = null;
        if (serverSocketTypeConf != null ) {
            confSocketType = serverSocketTypeConf.getValue();
        }

        if (confSocketType == null) {
            // Only load the useTLS parameter if a specific socket type has not
            // been specified.  This maintains backwards compatibility while
            // allowing us to have more complex (i.e. multiple SSL configuration)
            // deployments
            final boolean useTLS = conf.getChild("useTLS").getValueAsBoolean(isDefaultTLSEnabled());
            if (useTLS) {
                serverSocketType = "ssl";
                loadJCEProviders(conf, logger);
            }
        } else {
            serverSocketType = confSocketType;
        }

        StringBuffer infoBuffer;
        threadGroup = conf.getChild("threadGroup").getValue(null);
        if (threadGroup != null) {
            infoBuffer =
                new StringBuffer(64)
                        .append(getServiceType())
                        .append(" uses thread group: ")
                        .append(threadGroup);
            logger.info(infoBuffer.toString());
        }
        else {
            logger.info(getServiceType() + " uses default thread group.");
        }

        try {
            final String bindAddress = conf.getChild("bind").getValue(null);
            if( null != bindAddress ) {
                bindTo = InetAddress.getByName(bindAddress);
                infoBuffer =
                    new StringBuffer(64)
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
            new StringBuffer(64)
                    .append(getServiceType())
                    .append(" handler connection timeout is: ")
                    .append(timeout);
        logger.info(infoBuffer.toString());

        backlog = conf.getChild(BACKLOG_NAME).getValueAsInteger(DEFAULT_BACKLOG);

        infoBuffer =
                    new StringBuffer(64)
                    .append(getServiceType())
                    .append(" connection backlog is: ")
                    .append(backlog);
        logger.info(infoBuffer.toString());

        String connectionLimitString = conf.getChild("connectionLimit").getValue(null);
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
        infoBuffer = new StringBuffer(128)
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
        infoBuffer = new StringBuffer(128)
            .append(getServiceType())
            .append(" will allow a maximum of ")
            .append(connPerIP)
            .append(" per IP connections for " +getServiceType());
        logger.info(infoBuffer.toString());
        
    }

    private void loadJCEProviders(Configuration conf, final Logger logger) throws ConfigurationException {
        final Configuration [] providerConfiguration = conf.getChildren("provider");
        for (int i = 0; i < providerConfiguration.length; i++) {
            final String providerName = providerConfiguration[i].getValue();
            loadProvider(logger, providerName);
        }
    }

    private void loadProvider(final Logger logger, final String providerName) {
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

    private void logJCELoadFailure(final Logger logger, final String providerName, Exception e) {
        logger.warn("Cannot load JCE provider" + providerName);
        logger.debug(e.getMessage(), e);
    }

    protected void setStreamDumpDir(String streamdumpDir) {
        this.streamDumpDir = streamdumpDir;
    }

    protected String getLocalHostName() {
        String hostName = null;
        try {
            hostName = dnsServer.getHostName(dnsServer.getLocalHost());
        } catch (UnknownHostException ue) {
            hostName = "localhost";
        }
        return hostName;
    }
    
    private void configureHelloName(Configuration handlerConfiguration) {
        StringBuffer infoBuffer;
        String hostName = null;
        try {
            hostName = dnsServer.getHostName(dnsServer.getLocalHost());
        } catch (UnknownHostException ue) {
            hostName = "localhost";
        }

        infoBuffer =
            new StringBuffer(64)
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
            new StringBuffer(64)
                    .append(getServiceType())
                    .append(" handler hello name is: ")
                    .append(helloName);
        getLogger().info(infoBuffer.toString());
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @PostConstruct
    public final void initialize() throws Exception {
        if (!isEnabled()) {
            getLogger().info(getServiceType() + " Disabled");
            System.out.println(getServiceType() + " Disabled");
            return;
        }
        getLogger().debug(getServiceType() + " init...");

        prepareInit();
        
        // keeping these looked up services locally, because they are only needed beyond initialization
        ThreadManager threadManager = (ThreadManager) componentManager.lookup(ThreadManager.ROLE);
        SocketManager socketManager = (SocketManager) componentManager.lookup(SocketManager.ROLE);
        
        initializeThreadPool(threadManager);

        initializeServerSocket(socketManager);

        getLogger().debug(getServiceType() + " ...init end");

        initializeHandlerPool();
        
        // do avalon specific preparations
        ContainerUtil.enableLogging(theHandlerPool, getLogger());
        ContainerUtil.initialize(theHandlerPool);

        theWatchdogFactory = getWatchdogFactory();

        // Allow subclasses to perform initialisation
        doInit();
    }
    
    
    /**
     * Hook for subclasses to perform an required initialisation
     * before the superclass has been initialised.
     * Called before the super class has completed it's initialisation.
     * @throws Exception
     */
    protected void prepareInit() throws Exception {
        
    }
    
    /**
     * Hook for subclasses to perform an required initialisation.
     * Called after the super class has completed it's initialisation.
     * @throws Exception
     */
    protected void doInit() throws Exception {
        
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
            getLogger().fatalError(errorMessage);
            throw e;
        }       
    }
     
    private String getBindingErrorMessage(BindException e) {
        // general info about binding error
        StringBuffer errorMessage = new StringBuffer();
        errorMessage.append("FATAL ERROR when starting service '").append(getServiceType()).append("'! ");
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
            final StringBuffer sb = new StringBuffer();
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
        StringBuffer logBuffer =
                new StringBuffer(64)
                        .append(getServiceType())
                        .append(" started ")
                        .append(connectionName);
        String logString = logBuffer.toString();
        System.out.println(logString);
        getLogger().info(logString);

        if (connectionLimit != null) {
            theHandlerPool = new HardResourceLimitingPool(this, 5, connectionLimit.intValue());
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Using a bounded pool for "+getServiceType()+" handlers with upper limit " + connectionLimit.intValue());
            }
        } else {
            // NOTE: The maximum here is not a real maximum.  The handler pool will continue to
            //       provide handlers beyond this value.
            theHandlerPool = new DefaultPool(this, null, 5, 30);
            getLogger().debug("Using an unbounded pool for "+getServiceType()+" handlers.");
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {

        if (!isEnabled()) {
            return;
        }

        if( m_disposed )
        {
            if( getLogger().isWarnEnabled() )
            {
                getLogger().warn( "ignoring disposal request - already disposed" );
            }
            return;
        }

        if( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "disposal" );
        }

        m_disposed = true;
        if( getLogger().isDebugEnabled() )
        {
            StringBuffer infoBuffer =
               new StringBuffer(64).append(getServiceType()).append(
                   " dispose... ").append(connectionName);
            getLogger().debug(infoBuffer.toString());
        }

        try {
            connectionManager.disconnect(connectionName, true);
        } catch (final Exception e) {
            StringBuffer warnBuffer =
                new StringBuffer(64)
                        .append("Error disconnecting ")
                        .append(getServiceType())
                        .append(": ");
            getLogger().warn(warnBuffer.toString(), e);
        }

        componentManager = null;

        connectionManager = null;
        threadPool = null;

        // This is needed to make sure sockets are promptly closed on Windows 2000
        // TODO: Check this - shouldn't need to explicitly gc to force socket closure
        System.gc();

        getLogger().debug(getServiceType() + " ...dispose end");
    }

    /**
     * This constructs the WatchdogFactory that will be used to guard
     * against runaway or stuck behavior.  Should only be called once
     * by a subclass in its initialize() method.
     *
     * @return the WatchdogFactory to be employed by subclasses.
     */
    protected WatchdogFactory getWatchdogFactory() {
        WatchdogFactory theWatchdogFactory = null;
        theWatchdogFactory = new ThreadPerWatchdogFactory(threadPool, timeout);
        ContainerUtil.enableLogging(theWatchdogFactory,getLogger());
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
        
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Handler [" +  theHandler + "] obtained from pool.");
        }

        Watchdog theWatchdog = theWatchdogFactory.getWatchdog(theHandler);

        theHandler.setStreamDumpDir(streamDumpDir);
        theHandler.setWatchdog(theWatchdog);
        return theHandler;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory#releaseConnectionHandler(ConnectionHandler)
     */
    public void releaseConnectionHandler( ConnectionHandler connectionHandler ) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Returning Handler [" +  connectionHandler + "] to pool.");
        }
        theHandlerPool.put((Poolable)connectionHandler);
    }


    /**
     * Get the default port for this server type.
     *
     * It is strongly recommended that subclasses of this class
     * override this method to specify the default port for their
     * specific server type.
     *
     * @return the default port
     */
     protected int getDefaultPort() {
        return 0;
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
     * This method returns the type of service provided by this server.
     * This should be invariant over the life of the class.
     *
     * Subclasses may override this implementation.  This implementation
     * parses the complete class name and returns the undecorated class
     * name.
     *
     * @return description of this server
     */
    public String getServiceType() {
        String name = getClass().getName();
        int p = name.lastIndexOf(".");
        if (p > 0 && p < name.length() - 2) {
            name = name.substring(p + 1);
        }
        return name;
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
        ContainerUtil.service(conn, componentManager);
        return conn;
    }
    
    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public Object newInstance() throws Exception {
        final String name = "Handler-" + handlerCount.getAndAdd(1);
        final JamesConnectionBridge delegatingJamesHandler = 
            new JamesConnectionBridge(newProtocolHandlerInstance(), dnsServer, name, getLogger());
        return delegatingJamesHandler;
    }
    
    protected abstract ProtocolHandler newProtocolHandlerInstance();

   /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
     */
    public Class getCreatedClass() {
        return JamesConnectionBridge.class;
    }

}

