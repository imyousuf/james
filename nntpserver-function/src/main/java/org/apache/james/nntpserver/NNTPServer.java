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



package org.apache.james.nntpserver;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.services.MailServer;
import org.apache.james.socket.AbstractProtocolServer;
import org.apache.james.socket.api.ProtocolHandler;

/**
 * NNTP Server
 *
 */
public class NNTPServer extends AbstractProtocolServer implements NNTPServerMBean {

    /**
     * Whether authentication is required to access this NNTP server
     */
    private boolean authRequired = false;

    /**
     * The repository that stores the news articles for this NNTP server.
     */
    private NNTPRepository nntpRepository;

    /**
     * The repository that stores the local users.  Used for authentication.
     */
    private UsersRepository userRepository = null;

    private MailServer mailServer;

    /**
     * Set the UserRepository
     * 
     * @param userRepository the UserRepository
     */
    @Resource(name="org.apache.james.api.user.UsersRepository")
    public void setUserRepository(UsersRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Set the NNTPRepository
     * 
     * @param nntpRepository the NNTPRepository
     */
    @Resource(name="org.apache.james.nntpserver.repository.NNTPRepository")
    public void setNNTPRepository(NNTPRepository nntpRepository) {
        this.nntpRepository = nntpRepository;
    }
    
    @Resource(name="org.apache.james.services.MailServer")
    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * The configuration data to be passed to the handler
     */
    private NNTPHandlerConfigurationData theConfigData
        = new NNTPHandlerConfigurationDataImpl();

    

    @PostConstruct
    @Override
    public void init() throws Exception {
        super.init();
    }

    protected void onConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        if (isEnabled()) {
            authRequired = configuration.getBoolean("handler.authRequired", false);
            if (getLog().isDebugEnabled()) {
                if (authRequired) {
                    getLog().debug("NNTP Server requires authentication.");
                } else {
                    getLog().debug("NNTP Server doesn't require authentication.");
                }
            }
        }
    }

    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getDefaultPort()
     */
     public int getDefaultPort() {
        return 119;
     }

    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getServiceType()
     */
    public String getServiceType() {
        return "NNTP Service";
    }

    /**
     * @see org.apache.james.socket.AbstractProtocolServer#newProtocolHandlerInstance()
     */
    public ProtocolHandler newProtocolHandlerInstance() {
        final NNTPHandler handler = new NNTPHandler(theConfigData);
        return handler;
    }

    /**
     * A class to provide NNTP handler configuration to the handlers
     */
    private class NNTPHandlerConfigurationDataImpl
        implements NNTPHandlerConfigurationData {

        /**
         * @see org.apache.james.nntpserver.NNTPHandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            if (NNTPServer.this.getHelloName() == null) {
                return NNTPServer.this.mailServer.getHelloName();
            } else {
                return NNTPServer.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.nntpserver.NNTPHandlerConfigurationData#isAuthRequired()
         */
        public boolean isAuthRequired() {
            return NNTPServer.this.authRequired;
        }

        /**
         * @see org.apache.james.nntpserver.NNTPHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository() {
            return NNTPServer.this.userRepository;
        }

        /**
         * @see org.apache.james.nntpserver.NNTPHandlerConfigurationData#getNNTPRepository()
         */
        public NNTPRepository getNNTPRepository() {
            return NNTPServer.this.nntpRepository;
        }

    }
}
