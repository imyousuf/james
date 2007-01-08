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


package org.apache.james.services;


import org.apache.avalon.cornerstone.services.connection.ConnectionManager;
import java.net.ServerSocket;
import org.apache.excalibur.thread.ThreadPool;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;

/**
 * This interface extends the standard ConnectionManager interface to allow
 * connectionLimits to be specified on a per service basis
 **/
public interface JamesConnectionManager extends ConnectionManager
{
    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.JamesConnectionManager";

    /**
     * Returns the default maximum number of open connections supported by this
     * SimpleConnectionManager
     *
     * @return the maximum number of connections
     */
    int getMaximumNumberOfOpenConnections();

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
    void connect( String name,
                  ServerSocket socket,
                  ConnectionHandlerFactory handlerFactory,
                  ThreadPool threadPool,
                  int maxOpenConnections )
        throws Exception;
    
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
    void connect( String name,
                  ServerSocket socket,
                  ConnectionHandlerFactory handlerFactory,
                  int maxOpenConnections )
        throws Exception;

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
    void connect( String name,
                  ServerSocket socket,
                  ConnectionHandlerFactory handlerFactory,
                  ThreadPool threadPool )
        throws Exception;
    
    /**
     * Start managing a connection.
     * Management involves accepting connections and farming them out to threads
     * from pool to be handled.
     *
     * @param name the name of connection
     * @param socket the ServerSocket from which to
     * @param handlerFactory the factory from which to acquire handlers
     * @exception Exception if an error occurs
     */
    void connect( String name,
                  ServerSocket socket,
                  ConnectionHandlerFactory handlerFactory )
        throws Exception;
    

}
