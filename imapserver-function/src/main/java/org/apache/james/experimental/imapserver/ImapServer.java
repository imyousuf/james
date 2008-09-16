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

package org.apache.james.experimental.imapserver;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.imapserver.DefaultImapFactory;
import org.apache.james.services.FileSystem;
import org.apache.james.socket.AbstractJamesService;
import org.apache.james.socket.ProtocolHandler;

/**
 * TODO: this is a quick cut-and-paste hack from POP3Server. Should probably be
 * rewritten from scratch, together with ImapHandler.
 *
 * <p>Accepts IMAP connections on a server socket and dispatches them to IMAPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing IMAP specific configuration.</p>
 */
public class ImapServer extends AbstractJamesService implements ImapConstants
{
    private static final String softwaretype = "JAMES "+VERSION+" Server " + Constants.SOFTWARE_VERSION;
     
    private DefaultImapFactory factory;
    
    private String hello = softwaretype;

    public ImapServer()
    {
    }
    
    
    
    public void service(ServiceManager comp) throws ServiceException {
        super.service(comp);
        factory = new DefaultImapFactory((FileSystem) comp.lookup(FileSystem.ROLE), 
                (UsersRepository) comp.lookup(UsersRepository.ROLE), getLogger());
    }



    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration ) throws ConfigurationException
    {
        super.configure( configuration );
        hello  = softwaretype + " Server " + helloName + " is ready.";
    }
    
    /**
     * @see AbstractJamesService#getDefaultPort()
     */
    protected int getDefaultPort()
    {
        return 143;
    }

    /**
     * @see AbstractJamesService#getServiceType()
     */
    public String getServiceType()
    {
        return "IMAP Service";
    }

    /**
     * Producing handlers.
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public ProtocolHandler newProtocolHandlerInstance()
    {
        final ImapRequestHandler handler = factory.createHandler();
        final ImapHandler imapHandler = new ImapHandler(handler, hello); 
        getLogger().debug("Create handler instance");
        return imapHandler;
    }

    // TODO: 
    protected Object getConfigurationData() {
        return null;
    }
}
