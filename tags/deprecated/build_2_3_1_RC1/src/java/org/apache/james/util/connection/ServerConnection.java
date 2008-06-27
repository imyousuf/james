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

package org.apache.james.util.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.excalibur.pool.HardResourceLimitingPool;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.excalibur.thread.ThreadPool ;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;


/**
 * Represents a single server socket managed by a connection manager.
 * The connection manager will spawn a single ServerConnection for each
 * server socket that the connection manager is managing.
 *
 */
public class ServerConnection extends AbstractLogEnabled
    implements Initializable, Runnable {

    /**
     * This is a hack to deal with the fact that there appears to be
     * no platform-independent way to break out of a ServerSocket
     * accept() call.  On some platforms closing either the ServerSocket
     * itself, or its associated InputStream, causes the accept
     * method to exit.  Unfortunately, this behavior is not consistent
     * across platforms.  The deal with this, we introduce a polling
     * loop of 20 seconds for the server socket.  This introduces a
     * cost across platforms, but is necessary to maintain cross-platform
     * functionality.
     */
    private static int POLLING_INTERVAL = 20*1000;

    /**
     * The server socket which this connection is managing
     */
    private ServerSocket serverSocket;

    /**
     * The connection handler factory that generates connection
     * handlers to manage client connections to this server socket
     */
    private ConnectionHandlerFactory handlerFactory;

    /**
     * The pool that produces ClientConnectionRunners
     */
    private Pool runnerPool;

    /**
     * The factory used to provide ClientConnectionRunner objects
     */
    private ObjectFactory theRunnerFactory = new ClientConnectionRunnerFactory();

    /**
     * The thread pool used to spawn individual threads used to manage each
     * client connection.
     */
    private ThreadPool connThreadPool;

    /**
     * The timeout for client sockets spawned off this connection.
     */
    private int socketTimeout;

    /**
     * The maximum number of open client connections that this server
     * connection will allow.
     */
    private int maxOpenConn;

    /**
     * A collection of client connection runners.
     */
    private final ArrayList clientConnectionRunners = new ArrayList();

    /**
     * The thread used to manage this server connection.
     */
    private Thread serverConnectionThread;

    /**
     * The sole constructor for a ServerConnection.
     *
     * @param serverSocket the ServerSocket associated with this ServerConnection
     * @param handlerFactory the factory that generates ConnectionHandlers for the client
     *                       connections spawned off this ServerConnection
     * @param threadPool the ThreadPool used to obtain handler threads
     * @param timeout the client idle timeout for this ServerConnection's client connections
     * @param maxOpenConn the maximum number of open client connections allowed for this
     *                    ServerConnection
     */
    public ServerConnection(ServerSocket serverSocket,
                            ConnectionHandlerFactory handlerFactory,
                            ThreadPool threadPool,
                            int timeout,
                            int maxOpenConn) {
        this.serverSocket = serverSocket;
        this.handlerFactory = handlerFactory;
        connThreadPool = threadPool;
        socketTimeout = timeout;
        this.maxOpenConn = maxOpenConn;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        runnerPool = new HardResourceLimitingPool(theRunnerFactory, 5, maxOpenConn);
        ContainerUtil.enableLogging(runnerPool,getLogger());
        ContainerUtil.initialize(runnerPool);
    }

    /**
     * The dispose operation is called by the owning ConnectionManager
     * at the end of its lifecycle.  Cleans up the server connection, forcing
     * everything to finish.
     */
    public void dispose() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Disposing server connection..." + this.toString());
        }
        synchronized( this ) {
            if( null != serverConnectionThread ) {
                // Execution of this block means that the run() method
                // hasn't finished yet.  So we interrupt the thread
                // to terminate run() and wait for the run() method
                // to finish.  The notifyAll() at the end of run() will
                // wake this thread and allow dispose() to end.
                Thread thread = serverConnectionThread;
                serverConnectionThread = null;
                thread.interrupt();
                try {
                    serverSocket.close();
                } catch (IOException ie) {
                    // Ignored - we're doing this to break out of the
                    // accept.  This minimizes the time required to
                    // shutdown the server.  Unfortunately, this is
                    // not guaranteed to work on all platforms.  See
                    // the comments for POLLING_INTERVAL
                }
                try {
                    if (POLLING_INTERVAL > 0) {
                        wait(2L*POLLING_INTERVAL);
                    } else {
                        wait();
                    }
                } catch (InterruptedException ie) {
                    // Expected - just complete dispose()
                }
            }
            ContainerUtil.dispose(runnerPool);
            runnerPool = null;
        }

        getLogger().debug("Closed server connection - cleaning up clients - " + this.toString());

        synchronized (clientConnectionRunners) {
            Iterator runnerIterator = clientConnectionRunners.iterator();
            while( runnerIterator.hasNext() ) {
                ClientConnectionRunner runner = (ClientConnectionRunner)runnerIterator.next();
                runner.dispose();
                runner = null;
            }
            clientConnectionRunners.clear();
        }

        getLogger().debug("Cleaned up clients - " + this.toString());

    }

    /**
     * Returns a ClientConnectionRunner in the set managed by this ServerConnection object.
     *
     * @param clientConnectionRunner the ClientConnectionRunner to be added
     */
    private ClientConnectionRunner addClientConnectionRunner()
            throws Exception {
        synchronized (clientConnectionRunners) {
            ClientConnectionRunner clientConnectionRunner = (ClientConnectionRunner)runnerPool.get();
            clientConnectionRunners.add(clientConnectionRunner);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Adding one connection for a total of " + clientConnectionRunners.size());
            }
            return clientConnectionRunner;
        }
    }

    /**
     * Removes a ClientConnectionRunner from the set managed by this ServerConnection object.
     *
     * @param clientConnectionRunner the ClientConnectionRunner to be removed
     */
    private void removeClientConnectionRunner(ClientConnectionRunner clientConnectionRunner) {

       /*
        * checking runnerPool avoids 'dead-lock' when service is disposing :
        * (dispose() calls dispose all runners)
        * but runner is 'running' and cleans up on exit
        * this situation will result in a dead-lock on 'clientConnectionRunners'
        */
        if( runnerPool == null ) {
            getLogger().info("ServerConnection.removeClientConnectionRunner - dispose has been called - so just return : " + clientConnectionRunner );
            return;
        }
        
        synchronized (clientConnectionRunners) {
            if (clientConnectionRunners.remove(clientConnectionRunner)) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Releasing one connection, leaving a total of " + clientConnectionRunners.size());
                }
                runnerPool.put(clientConnectionRunner);
            }
        }

        synchronized (this) { notify(); } // match the wait(...) in the run() inner loop before accept().
    }

    /**
     * Provides the body for the thread of execution for a ServerConnection.
     * Connections made to the server socket are passed to an appropriate,
     * newly created, ClientConnectionRunner
     */
    public void run() {
        serverConnectionThread = Thread.currentThread();

        int ioExceptionCount = 0;
        try {
            serverSocket.setSoTimeout(POLLING_INTERVAL);
        } catch (SocketException se) {
            // Ignored - for the moment
        }

        if ((getLogger().isDebugEnabled()) && (serverConnectionThread != null)) {
            StringBuffer debugBuffer =
                new StringBuffer(128)
                    .append(serverConnectionThread.getName())
                    .append(" is listening on ")
                    .append(serverSocket.toString());
            getLogger().debug(debugBuffer.toString());
        }
        while( !Thread.currentThread().interrupted() && null != serverConnectionThread ) {
            try {
                Socket clientSocket = null;
                try {
                    while (maxOpenConn > 0 && clientConnectionRunners.size() >= maxOpenConn) {
                        getLogger().warn("Maximum number of open connections (" +  clientConnectionRunners.size() + ") in use.");
                        synchronized (this) { wait(10000); }
                    }

                    clientSocket = serverSocket.accept();

                } catch( InterruptedIOException iioe ) {
                    // This exception is expected upon ServerConnection shutdown.
                    // See the POLLING_INTERVAL comment
                    continue;
                } catch( IOException se ) {
                    if (ioExceptionCount > 0) {
                        getLogger().error( "Fatal exception while listening on server socket.  Terminating connection.", se );
                        break;
                    } else {
                        continue;
                    }
                } catch( SecurityException se ) {
                    getLogger().error( "Fatal exception while listening on server socket.  Terminating connection.", se );
                    break;
                }
                ClientConnectionRunner runner = null;
                synchronized (clientConnectionRunners) {
                    if ((maxOpenConn > 0) && (clientConnectionRunners.size() >= maxOpenConn)) {
                        if (getLogger().isWarnEnabled()) {
                           getLogger().warn("Maximum number of open connections exceeded - refusing connection.  Current number of connections is " + clientConnectionRunners.size());
                           if (getLogger().isWarnEnabled()) {
                               Iterator runnerIterator = clientConnectionRunners.iterator();
                               getLogger().info("Connections: ");
                               while( runnerIterator.hasNext() ) {
                                   getLogger().info("    " + ((ClientConnectionRunner)runnerIterator.next()).toString());
                               }
                           }
                        }
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {
                            // We ignore this exception, as we already have an error condition.
                        }
                        continue;
                    } else {
                        clientSocket.setSoTimeout(socketTimeout);
                        runner = addClientConnectionRunner();
                        runner.setSocket(clientSocket);
                    }
                }
                setupLogger( runner );
                try {
                    connThreadPool.execute( runner );
                } catch (Exception e) {
                    // This error indicates that the underlying thread pool
                    // is out of threads.  For robustness, we catch this and
                    // cleanup
                    getLogger().error("Internal error - insufficient threads available to service request.  " +
                                      Thread.activeCount() + " threads in service request pool.", e);
                    try {
                        clientSocket.close();
                    } catch (IOException ignored) {
                        // We ignore this exception, as we already have an error condition.
                    }
                    // In this case, the thread will not remove the client connection runner,
                    // so we must.
                    removeClientConnectionRunner(runner);
                }
            } catch( IOException ioe ) {
                getLogger().error( "Exception accepting connection", ioe );
            } catch( Throwable e ) {
                getLogger().error( "Exception executing client connection runner: " + e.getMessage(), e );
            }
        }
        synchronized( this ) {
            serverConnectionThread = null;
            Thread.currentThread().interrupted();
            notifyAll();
        }
    }

    /**
     * An inner class to provide the actual body of the thread of execution
     * that occurs upon a client connection.
     *
     */
    class ClientConnectionRunner extends AbstractLogEnabled
        implements Poolable, Runnable  {

        /**
         * The Socket that this client connection is using for transport.
         */
        private Socket clientSocket;

        /**
         * The thread of execution associated with this client connection.
         */
        private Thread clientSocketThread;

        /**
         * Returns string for diagnostic logging
         */
        public String toString() {
            return getClass().getName() + " for " + clientSocket + " on " + clientSocketThread;
        }

        public ClientConnectionRunner() {
        }

        /**
         * The dispose operation that terminates the runner.  Should only be
         * called by the ServerConnection that owns the ClientConnectionRunner
         */
        public void dispose() {
            synchronized( this ) {
                if (null != clientSocketThread) {
                    // Execution of this block means that the run() method
                    // hasn't finished yet.  So we interrupt the thread
                    // to terminate run() and wait for the run() method
                    // to finish.  The notifyAll() at the end of run() will
                    // wake this thread and allow dispose() to end.
                    clientSocketThread.interrupt();
                    clientSocketThread = null;
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        // Expected - return from the method
                    }
                }
            }
        }

        /**
         * Sets the socket for a ClientConnectionRunner.
         *
         * @param socket the client socket associated with this ClientConnectionRunner
         */
        public void setSocket(Socket socket) {
            clientSocket = socket;
        }

        /**
         * Provides the body for the thread of execution dealing with a particular client
         * connection.  An appropriate ConnectionHandler is created, applied, executed,
         * and released.
         */
        public void run() {
            ConnectionHandler handler = null;
            try {
                clientSocketThread = Thread.currentThread();

                handler = ServerConnection.this.handlerFactory.createConnectionHandler();
                String connectionString = null;
                if( getLogger().isDebugEnabled() ) {
                    connectionString = getConnectionString();
                    String message = "Starting " + connectionString;
                    getLogger().debug( message );
                }

                handler.handleConnection(clientSocket);

                if( getLogger().isDebugEnabled() ) {
                    String message = "Ending " + connectionString;
                    getLogger().debug( message );
                }

            } catch( Throwable e ) {
                getLogger().error( "Error handling connection", e );
            } finally {

                // Close the underlying socket
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch( IOException ioe ) {
                    getLogger().warn( "Error shutting down connection", ioe );
                }

                clientSocket = null;

                // Null out the thread, notify other threads to encourage
                // a context switch
                synchronized( this ) {
                    clientSocketThread = null;

                    Thread.currentThread().interrupted();

                    // Release the handler and kill the reference to the handler factory
                    //
                    // This needs to be done after the clientSocketThread is nulled out,
                    // otherwise we could trash a reused ClientConnectionRunner
                    if (handler != null) {
                        ServerConnection.this.handlerFactory.releaseConnectionHandler( handler );
                        handler = null;
                    }

                    // Remove this runner from the list of active connections.
                    ServerConnection.this.removeClientConnectionRunner(this);

                    notifyAll();
                }
            }
        }

        /**
         * Helper method to return a formatted string with connection transport information.
         *
         * @return a formatted string
         */
        private String getConnectionString() {
            if (clientSocket == null) {
                return "invalid socket";
            }
            StringBuffer connectionBuffer
                = new StringBuffer(256)
                    .append("connection on ")
                    .append(clientSocket.getLocalAddress().getHostAddress().toString())
                    .append(":")
                    .append(clientSocket.getLocalPort())
                    .append(" from ")
                    .append(clientSocket.getInetAddress().getHostAddress().toString())
                    .append(":")
                    .append(clientSocket.getPort());
            return connectionBuffer.toString();
        }
    }

    /**
     * The factory for producing handlers.
     */
    private class ClientConnectionRunnerFactory
        implements ObjectFactory {

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception {
            return new ClientConnectionRunner();
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass() {
            return ClientConnectionRunner.class;
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#decommision(Object)
         */
        public void decommission( Object object ) throws Exception {
            return;
        }
    }
}


