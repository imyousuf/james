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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.services.UsersRepository;

/**
 * NNTP Server
 *
 */
public class NNTPServer extends AbstractJamesService implements NNTPServerMBean {

    /**
     * Whether authentication is required to access this NNTP server
     */
    private boolean authRequired = false;

    /**
     * The repository that stores the news articles for this NNTP server.
     */
    private NNTPRepository repository;

    /**
     * The repository that stores the local users.  Used for authentication.
     */
    private UsersRepository userRepository = null;

    public void setUserRepository(UsersRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setRepository(NNTPRepository repository) {
        this.repository = repository;
    }

    /**
     * The configuration data to be passed to the handler
     */
    private NNTPHandlerConfigurationData theConfigData
        = new NNTPHandlerConfigurationDataImpl();

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        super.service(componentManager);
        UsersRepository userRepository = (UsersRepository)componentManager.lookup(UsersRepository.ROLE);
        setUserRepository(userRepository);

        NNTPRepository repo = (NNTPRepository)componentManager
            .lookup("org.apache.james.nntpserver.repository.NNTPRepository");
        setRepository(repo);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        super.configure(configuration);
        if (isEnabled()) {
            Configuration handlerConfiguration = configuration.getChild("handler");
            authRequired =
                handlerConfiguration.getChild("authRequired").getValueAsBoolean(false);
            if (getLogger().isDebugEnabled()) {
                if (authRequired) {
                    getLogger().debug("NNTP Server requires authentication.");
                } else {
                    getLogger().debug("NNTP Server doesn't require authentication.");
                }
            }
        }
    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getDefaultPort()
     */
     protected int getDefaultPort() {
        return 119;
     }

    /**
     * @see org.apache.james.core.AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "NNTP Service";
    }


    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public Object newInstance() throws Exception {
        return new NNTPHandler();
    }

    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
     */
    public Class getCreatedClass() {
        return NNTPHandler.class;
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
            return NNTPServer.this.helloName;
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
            return NNTPServer.this.repository;
        }

    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getConfigurationData()
     */
    protected Object getConfigurationData() {
        return theConfigData;
    }
}
