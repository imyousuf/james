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



package org.apache.james.smtpserver;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.smtpserver.core.CoreCmdHandlerLoader;
import org.apache.james.smtpserver.core.CoreMessageHookLoader;
import org.apache.james.smtpserver.core.DataLineMessageHookHandler;
import org.apache.james.socket.AbstractHandlerChain;
import org.apache.james.socket.LogEnabled;

/**
  * The SMTPHandlerChain is per service object providing access
  * ConnectHandlers, Command handlers and message handlers
  */
public class SMTPHandlerChain extends AbstractHandlerChain implements LogEnabled{

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(DataLineMessageHookHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log log = FALLBACK_LOG;
   
    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * loads the various handlers from the configuration
     * 
     * @param configuration
     *            configuration under handlerchain node
     */
    @SuppressWarnings("unchecked")
	protected void loadHandlers() throws Exception {
        super.loadHandlers();
        if (commonsConf != null) {
            List<org.apache.commons.configuration.Configuration> children = commonsConf.configurationsAt("handler");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // load the configured handlers
            if (children != null && children.isEmpty() == false) {

                // load core messageHandlers
                loadClass(classLoader, CoreMessageHookLoader.class.getName(),
                        addHandler(CoreMessageHookLoader.class.getName()));

            }
        }
    }

    /**
     * @see org.apache.james.socket.AbstractHandlerChain#getLog()
     */
    protected Log getLog() {
        return log;
    }

    /**
     * @see org.apache.james.socket.AbstractHandlerChain#getCoreCmdHandlerLoader()
     */
    protected Class<?> getCoreCmdHandlerLoader() {
        return CoreCmdHandlerLoader.class;
    }

}
