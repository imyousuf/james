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



package org.apache.james.pop3server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.pop3server.core.CoreCmdHandlerLoader;
import org.apache.james.socket.AbstractHandlerChain;
import org.apache.james.socket.LogEnabled;

/**
  * The POP3HandlerChain is per service object providing access
  * ConnectHandlers and Commandhandlers
  */
public class POP3HandlerChain extends AbstractHandlerChain implements LogEnabled{

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(POP3HandlerChain.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log log  = FALLBACK_LOG;
    

    /**
     * @see org.apache.james.socket.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
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
