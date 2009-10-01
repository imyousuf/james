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



package org.apache.james.smtpserver.core;

import org.apache.james.socket.HandlersPackage;

import java.util.LinkedList;
import java.util.List;

/**
 * This class represent the base message hooks which are shipped with james.
 */
public class CoreMessageHookLoader implements HandlersPackage {

    private final String ADDDEFAULTATTRIBUTESHANDLER = AddDefaultAttributesMessageHook.class.getName();
    private final String SENDMAILHANDLER = SendMailHandler.class.getName();
    
    /**
     * @see org.apache.james.socket.HandlersPackage#getHandlers()
     */
    public List<String> getHandlers() {
        List<String> commands = new LinkedList<String>();
        
        // Add the default messageHooks
        commands.add(ADDDEFAULTATTRIBUTESHANDLER);
        commands.add(SENDMAILHANDLER);

        return commands;
    }
}
