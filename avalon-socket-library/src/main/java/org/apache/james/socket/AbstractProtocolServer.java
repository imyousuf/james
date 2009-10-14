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

import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.socket.shared.ProtocolHandler;
import org.apache.james.socket.shared.ProtocolHandlerFactory;
import org.apache.james.socket.shared.ProtocolServer;

/**
 * Server which creates connection handlers. All new James service must
 * inherit from this abstract implementation.
 *
 */
public abstract class AbstractProtocolServer extends AvalonProtocolServer implements ProtocolHandlerFactory {

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        DefaultServiceManager sm = new DefaultServiceManager(comp);
        sm.put(ProtocolHandlerFactory.ROLE, (ProtocolHandlerFactory) this);
        super.service(sm);
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

    /**
     * Get the default port for this server type.
     *
     * It is strongly recommended that subclasses of this class
     * override this method to specify the default port for their
     * specific server type.
     *
     * @return the default port
     */
     public abstract int getDefaultPort();

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
    public abstract String getServiceType();
    
    public abstract ProtocolHandler newProtocolHandlerInstance();

    /**
     * @see org.apache.james.socket.shared.ProtocolHandlerFactory#prepare(org.apache.james.socket.shared.ProtocolServer)
     */
    public void prepare(ProtocolServer server) throws Exception {
        prepareInit();
    }

    /**
     * @see org.apache.james.socket.shared.ProtocolHandlerFactory#init()
     */
    public void init() throws Exception {
        doInit();
    }

}

