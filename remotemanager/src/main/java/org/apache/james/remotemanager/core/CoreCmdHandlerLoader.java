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

package org.apache.james.remotemanager.core;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.protocols.api.HandlersPackage;

public class CoreCmdHandlerLoader implements HandlersPackage{
    private final List<String> commands = new LinkedList<String>();
    private final static String ADDDOMAINCMDHANDLER = AddDomainCmdHandler.class.getName();
    private final static String ADDMAPPINGCMDHANDLER = AddMappingCmdHandler.class.getName();
    private final static String ADDUSERCMDHANDLER = AddUserCmdHandler.class.getName();
    private final static String COUNTUSERSCMDHANDLER = CountUsersCmdHandler.class.getName();
    private final static String DELUSERCMDHANDLER = DelUserCmdHandler.class.getName();
    private final static String HELPCMDHANDLER = HelpCmdHandler.class.getName();
    private final static String LISTALLMAPPINGSCMDHANDLER = ListAllMappingsCmdHandler.class.getName();
    private final static String LISTDOMAINSCMDHANDLER = ListDomainsCmdHandler.class.getName();
    private final static String LISTMAPPINGCMDHANDLER = ListMappingCmdHandler.class.getName();
    private final static String LISTUSERSCMDHANDLER = ListUsersCmdHandler.class.getName();
    private final static String MEMSTATCMDHANDLER = MemStatCmdHandler.class.getName();
    private final static String QUITCMDHANDLER = QuitCmdHandler.class.getName();
    private final static String REMOVEDOMAINCMDHANDLER = RemoveDomainCmdHandler.class.getName();
    private final static String REMOVEMAPPINGCMDHANDLER = RemoveMappingCmdHandler.class.getName();
    private final static String SETPASSWORDCMDHANDLER = SetPasswordCmdHandler.class.getName();
    private final static String SHOWALIASCMDHANDLER = ShowAliasCmdHandler.class.getName();
    private final static String SHOWFORWARDINGCMDHANDLER = ShowForwardingCmdHandler.class.getName();
    private final static String SHUTDOWNCMDHANDLER = ShutdownCmdHandler.class.getName();
    private final static String UNKNOWNCMDHANDLER = UnknownCmdHandler.class.getName();
    private final static String UNSETALIASCMDHANDLER = UnsetAliasCmdHandler.class.getName();
    private final static String UNSETFORWARDINGCMDHANDLER = UnsetForwardingCmdHandler.class.getName();
    private final static String VERIFYCMDHANDLER = VerifyCmdHandler.class.getName();
    private final static String COMMANDDISPATCHER = RemoteManagerCommandDispatcherLineHandler.class.getName();
    private final static String AUTHORIZATIONHANDLER = AuthorizationHandler.class.getName();

    public CoreCmdHandlerLoader() {
        // Insert the base commands in the Map
        commands.add(COMMANDDISPATCHER);
        commands.add(AUTHORIZATIONHANDLER);

        commands.add(ADDDOMAINCMDHANDLER);
        commands.add(ADDMAPPINGCMDHANDLER);
        commands.add(ADDUSERCMDHANDLER);
        commands.add(COUNTUSERSCMDHANDLER);
        commands.add(DELUSERCMDHANDLER);
        commands.add(HELPCMDHANDLER);
        commands.add(LISTALLMAPPINGSCMDHANDLER);
        commands.add(LISTDOMAINSCMDHANDLER);
        commands.add(LISTMAPPINGCMDHANDLER);
        commands.add(LISTUSERSCMDHANDLER);
        commands.add(MEMSTATCMDHANDLER);
        commands.add(QUITCMDHANDLER);
        commands.add(REMOVEDOMAINCMDHANDLER);
        commands.add(REMOVEMAPPINGCMDHANDLER);
        commands.add(SETPASSWORDCMDHANDLER);
        commands.add(SHOWALIASCMDHANDLER);
        commands.add(SHOWFORWARDINGCMDHANDLER);
        commands.add(SHUTDOWNCMDHANDLER);
        commands.add(UNKNOWNCMDHANDLER);
        commands.add(UNSETALIASCMDHANDLER);
        commands.add(UNSETFORWARDINGCMDHANDLER);
        commands.add(VERIFYCMDHANDLER);

        
    }
    
    /**
     * @see org.apache.james.api.protocol.HandlersPackage#getHandlers()
     */
    public List<String> getHandlers() {
        return commands;
    }

}
