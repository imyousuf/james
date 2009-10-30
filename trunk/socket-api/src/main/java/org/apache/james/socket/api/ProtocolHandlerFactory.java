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

package org.apache.james.socket.api;


/**
 * An interface each protocol factory will expose to socket services.
 */
public interface ProtocolHandlerFactory {

    public final static String ROLE = "org.apache.james.socket.shared.ProtocolHandlerFactory";

    /**
     * Get the default port for this server type.
     * 
     * It is strongly recommended that subclasses of this class override this
     * method to specify the default port for their specific server type.
     * 
     * @return the default port
     */
    int getDefaultPort();

    /**
     * This method returns the type of service provided by this server. This
     * should be invariant over the life of the class.
     * 
     * Subclasses may override this implementation. This implementation parses
     * the complete class name and returns the undecorated class name.
     * 
     * @return description of this server
     */
    String getServiceType();

    /**
     * This is the factory method to obtain a new instance of a ProtocolHandler.
     * 
     * @return a new ProtocolHandler instance
     */
    ProtocolHandler newProtocolHandlerInstance();

    /**
     * Hook for protocol factories to perform an required initialisation before
     * the socket handler has been initialised. Called before the socket handler
     * has completed it's initialisation.
     * 
     * @throws Exception
     */
    void prepare(ProtocolServer server) throws Exception;

    /**
     * Hook for protocol factories to perform the initialisation after the
     * socket handler has been initialized TODO maybe this is not required
     * 
     * @throws Exception
     */
    void doInit() throws Exception;

}