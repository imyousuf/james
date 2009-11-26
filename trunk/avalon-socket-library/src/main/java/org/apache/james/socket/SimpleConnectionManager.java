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
import java.net.ServerSocket;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.excalibur.thread.ThreadPool;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.container.ContainerUtil;
/**
 * An implementation of ConnectionManager that supports configurable
 * idle timeouts and a configurable value for the maximum number of
 * client connections to a particular port.
 *
 */
public class SimpleConnectionManager implements JamesConnectionManager {
    /**
     * The default value for client socket idle timeouts.  The
     * Java default is 0, meaning no timeout.  That's dangerous
     * for a connection handler like this one, because it can
     * easily lead to consumption of network resources.  So we
     * allow users to configure the system to allow no timeout,
     * but if no timeout is specified in the configuration, we
     * use a timeout of 5 minutes.
     */
    private static final int DEFAULT_SOCKET_TIMEOUT = 5 * 60 * 1000;
    /**
     * The default value for the maximum number of allowed client
     * connections.
     */
    private static final int DEFAULT_MAX_CONNECTIONS = 30;
    /**
     * The default value for the maximum number of allowed client
     * connections.
     */
    private static final int DEFAULT_MAX_CONNECTIONS_PER_IP = 0;
    
    /**
     * The map of connection name / server connections managed by this connection
     * manager.
     */
    private final HashMap<String, ServerConnection> connectionMap = new HashMap<String, ServerConnection>();
    /**
     * The idle timeout for the individual sockets spawned from the server socket.
     */
    protected int timeout = 0;
    /**
     * The maximum number of client connections allowed per server connection.
     */
    protected int maxOpenConn = 0;
    /**
     * The maximum number of client connections allowed per server connection per IP.
     */
    protected int maxOpenConnPerIP = 0;
    /**
     * The ThreadManager component that is used to provide a default thread pool.
     */
    private ThreadManager threadManager;
    /**
     * Whether the SimpleConnectionManager has been disposed.
     */
    private volatile boolean disposed = false;
    private Log logger;
    private HierarchicalConfiguration configuration;

    @Resource(name="org.apache.avalon.cornerstone.services.threads.ThreadManager")
    public void setThreadManager(ThreadManager threadManager) {
        this.threadManager = threadManager;
    }
    

    @Resource(name="org.apache.commons.logging.Log")
    public void setLog(Log logger) {
        this.logger = logger;
    }
   
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    

    @PostConstruct
    public void init() throws Exception {
        timeout = configuration.getInt("idle-timeout", DEFAULT_SOCKET_TIMEOUT);
        maxOpenConn = configuration.getInt("max-connections", DEFAULT_MAX_CONNECTIONS);
        maxOpenConnPerIP = configuration.getInt("max-connections-per-ip", DEFAULT_MAX_CONNECTIONS_PER_IP);
        if (timeout < 0) {
            StringBuilder exceptionBuffer =
                new StringBuilder(128).append("Specified socket timeout value of ").append(timeout).append(
                    " is not a legal value.");
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        if (maxOpenConn < 0) {
            StringBuilder exceptionBuffer =
                new StringBuilder(128).append("Specified maximum number of open connections of ").append(
                    maxOpenConn).append(
                    " is not a legal value.");
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        if (maxOpenConnPerIP < 0) {
            StringBuilder exceptionBuffer =
                new StringBuilder(128).append("Specified maximum number of open connections per IP of ").append(
                    maxOpenConnPerIP).append(
                    " is not a legal value.");
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        if (logger.isDebugEnabled()) {
            logger.debug(
                "Connection timeout is " + (timeout == 0 ? "unlimited" : Long.toString(timeout)));
            logger.debug(
                "The maximum number of simultaneously open connections is "
                    + (maxOpenConn == 0 ? "unlimited" : Integer.toString(maxOpenConn)));
        }
    }
    
    
    /**
     * Disconnects all the underlying ServerConnections
     */
    @PreDestroy
    public void dispose() {
        disposed = true;
        if (logger.isDebugEnabled()) {
            logger.debug("Starting SimpleConnectionManager dispose...");
        }
        final String[] names = (String[])connectionMap.keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Disconnecting ServerConnection " + names[i]);
                }
                disconnect(names[i], true);
            } catch (final Exception e) {
                logger.warn("Error disconnecting " + names[i], e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Finishing SimpleConnectionManager dispose...");
        }
    }
    
    
    /**
     * Start managing a connection.
     * Management involves accepting connections and farming them out to threads
     * from pool to be handled.
     *
     * @param name the name of connection
     * @param socket the ServerSocket from which to
     * @param handlerFactory the factory from which to acquire handlers
     * @param threadPool the thread pool to use
     * @param maxOpenConnections the maximum number of open connections allowed for this server socket.
     * @param maxOpenConnectionsPerIP the maximum number of open connections per IP allowed for this server socket.
     * @exception Exception if an error occurs
     */
    public void connect(
        String name,
        ServerSocket socket,
        ConnectionHandlerFactory handlerFactory,
        ThreadPool threadPool,
        int maxOpenConnections,
        int maxOpenConnectionsPerIP)
        throws Exception {
        if (disposed) {
            throw new IllegalStateException("Connection manager has already been shutdown.");
        }
        if (null != connectionMap.get(name)) {
            throw new IllegalArgumentException("Connection already exists with name " + name);
        }
        if (maxOpenConnections < 0) {
            throw new IllegalArgumentException("The maximum number of client connections per server socket cannot be less that zero.");
        } 
        if (maxOpenConnectionsPerIP < 0) {
            throw new IllegalArgumentException("The maximum number of client connections (per IP) per server socket cannot be less that zero.");
        }
        ServerConnection runner =
            new ServerConnection(socket, handlerFactory, threadPool, logger, timeout, maxOpenConnections, maxOpenConnectionsPerIP);
        runner.init();
        connectionMap.put(name, runner);
        threadPool.execute(runner);
    }
    
    
    /**
     * Start managing a connection.
     * Management involves accepting connections and farming them out to threads
     * from pool to be handled.
     *
     * @param name the name of connection
     * @param socket the ServerSocket from which to
     * @param handlerFactory the factory from which to acquire handlers
     * @param threadPool the thread pool to use
     * @exception Exception if an error occurs
     */
    public void connect(
        String name,
        ServerSocket socket,
        ConnectionHandlerFactory handlerFactory,
        ThreadPool threadPool)
        throws Exception {
        connect(name, socket, handlerFactory, threadPool, maxOpenConn, maxOpenConnPerIP);
    }
    
    
    /**
     * Start managing a connection.
     * This is similar to other connect method except that it uses default thread pool.
     *
     * @param name the name of connection
     * @param socket the ServerSocket from which to
     * @param handlerFactory the factory from which to acquire handlers
     * @exception Exception if an error occurs
     */
    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory)
        throws Exception {
        connect(name, socket, handlerFactory, threadManager.getDefaultThreadPool());
    }
    
    
    /**
     * Start managing a connection.
     * This is similar to other connect method except that it uses default thread pool.
     *
     * @param name the name of connection
     * @param socket the ServerSocket from which to
     * @param handlerFactory the factory from which to acquire handlers
     * @param maxOpenConnections the maximum number of open connections allowed for this server socket.
     * @exception Exception if an error occurs
     */
    public void connect(
        String name,
        ServerSocket socket,
        ConnectionHandlerFactory handlerFactory,
        int maxOpenConnections)
        throws Exception {
        connect(name, socket, handlerFactory, threadManager.getDefaultThreadPool(), maxOpenConnections);
    }
    
    
    /**
     * This shuts down all handlers and socket, waiting for each to gracefully shutdown.
     *
     * @param name the name of connection
     * @exception Exception if an error occurs
     */
    public void disconnect(final String name) throws Exception {
        disconnect(name, false);
    }
    
    
    /**
     * This shuts down a connection.
     * If tearDown is true then it will forcefully the connection and try
     * to return as soon as possible. Otherwise it will behave the same as
     * void disconnect( String name );
     *
     * @param name the name of connection
     * @param tearDown if true will forcefully tear down all handlers
     * @exception Exception if an error occurs
     */
    public void disconnect(final String name, final boolean tearDown) throws Exception {
        ServerConnection connection = (ServerConnection)connectionMap.remove(name);
        if (null == connection) {
            throw new IllegalArgumentException("No such connection with name " + name);
        }
        // TODO: deal with tear down parameter
        connection.dispose();
    }
    
  
    /**
     * @see org.apache.james.socket.JamesConnectionManager#connect(java.lang.String, java.net.ServerSocket, org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory, org.apache.excalibur.thread.ThreadPool, int)
     */
    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, ThreadPool threadPool, int maxOpenConnections) throws Exception {
        connect(name,socket,handlerFactory,threadPool,maxOpenConnections,maxOpenConnPerIP);
    }
  
    
    /**
     * @see org.apache.james.socket.JamesConnectionManager#connect(java.lang.String, java.net.ServerSocket, org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory, int, int)
     */
    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, int maxOpenConnections,int maxOpenConnectionsPerIP) throws Exception {
        connect(name,socket,handlerFactory,threadManager.getDefaultThreadPool(),maxOpenConnections,maxOpenConnectionsPerIP);
    }
    
    
    /**
     * Returns the default maximum number of open connections supported by this
     * SimpleConnectionManager
     *
     * @return the maximum number of connections
     */
    public int getMaximumNumberOfOpenConnections() {
        return maxOpenConn;
    }
    
    
    /**
     * @see org.apache.james.socket.JamesConnectionManager#getMaximumNumberOfOpenConnectionsPerIP()
     */
    public int getMaximumNumberOfOpenConnectionsPerIP() {
        return maxOpenConnPerIP;
    }

}
