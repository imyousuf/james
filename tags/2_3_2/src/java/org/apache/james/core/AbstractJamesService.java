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

package org.apache.james.core;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

import org.apache.excalibur.thread.ThreadPool;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;

import org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.sockets.ServerSocketFactory;
import org.apache.avalon.cornerstone.services.sockets.SocketManager;

import org.apache.james.services.JamesConnectionManager;
import org.apache.james.util.watchdog.ThreadPerWatchdogFactory;
import org.apache.james.util.watchdog.WatchdogFactory;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

/**
 * Server which creates connection handlers. All new James service must
 * inherit from this abstract implementation.
 *
 */
public abstract class AbstractJamesService extends AbstractHandlerFactory
    implements Serviceable, Configurable, Disposable, Initializable, ConnectionHandlerFactory {

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

    /*
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
    private ServiceManager compMgr;

    /**
     * Whether this service is enabled.
     */
    private volatile boolean enabled;

    /**
     * Flag holding the disposed state of the component.
     */
    private boolean m_disposed = false;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        super.service( comp );
        compMgr               = comp;
        connectionManager =
            (JamesConnectionManager)compMgr.lookup(JamesConnectionManager.ROLE);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        enabled = conf.getAttributeAsBoolean("enabled", true);
        if (!enabled) {
          getLogger().info(getServiceType() + " disabled by configuration");
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
            getLogger().info(infoBuffer.toString());
        }
        else {
            getLogger().info(getServiceType() + " uses default thread group.");
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
                getLogger().info(infoBuffer.toString());
            }
        }
        catch( final UnknownHostException unhe ) {
            throw new ConfigurationException( "Malformed bind parameter in configuration of service " + getServiceType(), unhe );
        }

        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
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
        boolean autodetect = helloConf.getAttributeAsBoolean("autodetect", true);
        if (autodetect) {
            helloName = hostName;
        } else {
            helloName = helloConf.getValue("localhost");
        }
        infoBuffer =
            new StringBuffer(64)
                    .append(getServiceType())
                    .append(" handler hello name is: ")
                    .append(helloName);
        getLogger().info(infoBuffer.toString());

        timeout = handlerConfiguration.getChild(TIMEOUT_NAME).getValueAsInteger(DEFAULT_TIMEOUT);

        infoBuffer =
            new StringBuffer(64)
                    .append(getServiceType())
                    .append(" handler connection timeout is: ")
                    .append(timeout);
        getLogger().info(infoBuffer.toString());

        backlog = conf.getChild(BACKLOG_NAME).getValueAsInteger(DEFAULT_BACKLOG);

        infoBuffer =
                    new StringBuffer(64)
                    .append(getServiceType())
                    .append(" connection backlog is: ")
                    .append(backlog);
        getLogger().info(infoBuffer.toString());

        if (connectionManager instanceof JamesConnectionManager) {
            String connectionLimitString = conf.getChild("connectionLimit").getValue(null);
            if (connectionLimitString != null) {
                try {
                    connectionLimit = new Integer(connectionLimitString);
                } catch (NumberFormatException nfe) {
                    getLogger().error("Connection limit value is not properly formatted.", nfe);
                }
                if (connectionLimit.intValue() < 0) {
                    getLogger().error("Connection limit value cannot be less than zero.");
                    throw new ConfigurationException("Connection limit value cannot be less than zero.");
                }
            } else {
                connectionLimit = new Integer(((JamesConnectionManager)connectionManager).getMaximumNumberOfOpenConnections());
            }
            infoBuffer = new StringBuffer(128)
                .append(getServiceType())
                .append(" will allow a maximum of ")
                .append(connectionLimit.intValue())
                .append(" connections.");
            getLogger().info(infoBuffer.toString());
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        if (!isEnabled()) {
            getLogger().info(getServiceType() + " Disabled");
            System.out.println(getServiceType() + " Disabled");
            return;
        }
        getLogger().debug(getServiceType() + " init...");

        SocketManager socketManager = (SocketManager) compMgr.lookup(SocketManager.ROLE);

        ThreadManager threadManager = (ThreadManager) compMgr.lookup(ThreadManager.ROLE);

        if (threadGroup != null) {
            threadPool = threadManager.getThreadPool(threadGroup);
        } else {
            threadPool = threadManager.getDefaultThreadPool();
        }

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

        if ((connectionLimit != null) &&
            (connectionManager instanceof JamesConnectionManager)) {
            if (null != threadPool) {
                ((JamesConnectionManager)connectionManager).connect(connectionName, serverSocket, this, threadPool, connectionLimit.intValue());
            }
            else {
                ((JamesConnectionManager)connectionManager).connect(connectionName, serverSocket, this, connectionLimit.intValue()); // default pool
            }
        } else {
            if (null != threadPool) {
                connectionManager.connect(connectionName, serverSocket, this, threadPool);
            }
            else {
                connectionManager.connect(connectionName, serverSocket, this); // default pool
            }
        }

        getLogger().debug(getServiceType() + " ...init end");

        StringBuffer logBuffer =
            new StringBuffer(64)
                .append(getServiceType())
                .append(" started ")
                .append(connectionName);
        String logString = logBuffer.toString();
        System.out.println(logString);
        getLogger().info(logString);
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

        compMgr = null;

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
        if (theWatchdogFactory instanceof LogEnabled) {
            ((LogEnabled)theWatchdogFactory).enableLogging(getLogger());
        }
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
     * Overide this method to create actual instance of connection handler.
     *
     * @return the new ConnectionHandler
     * @exception Exception if an error occurs
     */
    protected abstract ConnectionHandler newHandler()
        throws Exception;

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
    * @return String The scoekt type, plain or SSL     
    */  
    public String  getSocketType() {
        return serverSocketType;
    }
}

