/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.util.connection;

import java.net.ServerSocket;
import java.util.HashMap;

import org.apache.avalon.excalibur.thread.ThreadPool;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.james.services.JamesConnectionManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;


/**
 * An implementation of ConnectionManager that supports configurable
 * idle timeouts and a configurable value for the maximum number of 
 * client connections to a particular port.
 *
 * @author Andrei Ivanov
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class SimpleConnectionManager extends AbstractLogEnabled
    implements JamesConnectionManager, Composable, Configurable, Disposable {

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
     * The map of connection name / server connections managed by this connection
     * manager.
     */
    private final HashMap connectionMap = new HashMap();
  
    /**
     * The idle timeout for the individual sockets spawed from the server socket.
     */
    protected int timeout = 0;

    /**
     * The maximum number of client connections allowed per server connection.
     */
    protected int maxOpenConn = 0;

    /**
     * The ThreadManager component that is used to provide a default thread pool.
     */
    private ThreadManager threadManager;

    /**
     * Whether the SimpleConnectionManager has been disposed.
     */
    private volatile boolean disposed = false;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        timeout = configuration.getChild("idle-timeout").getValueAsInteger(DEFAULT_SOCKET_TIMEOUT);
        maxOpenConn = configuration.getChild("max-connections").getValueAsInteger(DEFAULT_MAX_CONNECTIONS);

        if (timeout < 0) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                    .append("Specified socket timeout value of ")
                    .append(timeout)
                    .append(" is not a legal value.");
            throw new ConfigurationException(exceptionBuffer.toString());
        } 

        if (maxOpenConn < 0) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                    .append("Specified maximum number of open connections of ")
                    .append(maxOpenConn)
                    .append(" is not a legal value.");
            throw new ConfigurationException(exceptionBuffer.toString());
        } 

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Connection timeout is " 
                             + (timeout == 0 ? "unlimited" : Long.toString(timeout)));
            getLogger().debug("The maximum number of simultaneously open connections is " 
                             + (maxOpenConn == 0 ? "unlimited" : Integer.toString(maxOpenConn)));
        }
    }

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager) 
        throws ComponentException {
        threadManager = (ThreadManager)componentManager.lookup( ThreadManager.ROLE );
    }

    /**
     * Disconnects all the underlying ServerConnections
     */
    public void dispose() {
        disposed = true;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Starting SimpleConnectionManager dispose...");
        }
        final String[] names = (String[])connectionMap.keySet().toArray( new String[ 0 ] );
        for( int i = 0; i < names.length; i++ ) {
            try {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Disconnecting ServerConnection " + names[i]);
                }
                disconnect( names[ i ], true);
            } catch( final Exception e ) {
                getLogger().warn( "Error disconnecting " + names[ i ], e );
            }
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Finishing SimpleConnectionManager dispose...");
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
     * @exception Exception if an error occurs
     */
    public void connect( String name,
                         ServerSocket socket,
                         ConnectionHandlerFactory handlerFactory,
                         ThreadPool threadPool,
                         int maxOpenConnections )
        throws Exception {

        if (disposed) {
            throw new IllegalStateException("Connection manager has already been shutdown.");
        }
        if( null != connectionMap.get( name ) ) {
            throw new IllegalArgumentException( "Connection already exists with name " +
                                                name );
        }
        if (maxOpenConnections < 0) {
            throw new IllegalArgumentException( "The maximum number of client connections per server socket cannot be less that zero.");
        }
        ServerConnection runner = new ServerConnection(socket, handlerFactory, threadPool, timeout, maxOpenConnections);
        setupLogger( runner );
        if (runner instanceof Initializable) {
            ((Initializable)runner).initialize();
        }
        connectionMap.put( name, runner );
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
    public void connect( String name,
                         ServerSocket socket,
                         ConnectionHandlerFactory handlerFactory,
                         ThreadPool threadPool )
        throws Exception {

        connect(name, socket, handlerFactory, threadPool, maxOpenConn);
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
    public void connect( String name,
                         ServerSocket socket,
                         ConnectionHandlerFactory handlerFactory )
        throws Exception {
        connect( name, socket, handlerFactory, threadManager.getDefaultThreadPool() );
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
    public void connect( String name,
                         ServerSocket socket,
                         ConnectionHandlerFactory handlerFactory,
                         int maxOpenConnections )
        throws Exception {
        connect( name, socket, handlerFactory, threadManager.getDefaultThreadPool(), maxOpenConnections );
    }

    /**
     * This shuts down all handlers and socket, waiting for each to gracefully shutdown.
     *
     * @param name the name of connection
     * @exception Exception if an error occurs
     */
    public void disconnect( final String name )
        throws Exception {
        disconnect( name, false );
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
    public void disconnect( final String name, final boolean tearDown )
        throws Exception {

        ServerConnection connection = (ServerConnection)connectionMap.remove( name );
        if( null == connection ) {
            throw new IllegalArgumentException( "No such connection with name " +
                                                name );
        }

        // TODO: deal with tear down parameter
        connection.dispose();
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
}
