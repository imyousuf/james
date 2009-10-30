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

import javax.annotation.Resource;

import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.api.vut.management.VirtualUserTableManagementService;
import org.apache.james.remotemanager.CommandHandler;

public abstract class AbstractMappingCmdHandler implements CommandHandler {

    protected final static String ADD_MAPPING_ACTION = "ADDMAPPING";
    protected final static String REMOVE_MAPPING_ACTION = "REMOVEMAPPING";
    protected VirtualUserTableManagementService vutManagement;

    @Resource(name = "org.apache.james.api.vut.management.VirtualUserTableManagementService")
    public final void setVirtualUserTableManagementService(VirtualUserTableManagementService vutManagement) {
        this.vutManagement = vutManagement;
    }

    protected boolean mappingAction(String[] args, String action) throws IllegalArgumentException, VirtualUserTableManagementException {
        String table = null;
        String user = null;
        String domain = null;
        String mapping = null;

        if (args[0].startsWith("table=")) {
            table = args[0].substring("table=".length());
            if (args[1].indexOf("@") > 0) {
                user = getMappingValue(args[1].split("@")[0]);
                domain = getMappingValue(args[1].split("@")[1]);
            } else {
                throw new IllegalArgumentException("Invalid usage.");
            }
            mapping = args[2];
        } else {
            if (args[0].indexOf("@") > 0) {
                user = getMappingValue(args[0].split("@")[0]);
                domain = getMappingValue(args[0].split("@")[1]);
            } else {
                throw new IllegalArgumentException("Invalid usage.");
            }
            mapping = args[1];
        }

        if (action.equals(ADD_MAPPING_ACTION)) {
            return vutManagement.addMapping(table, user, domain, mapping);
        } else if (action.equals(REMOVE_MAPPING_ACTION)) {
            return vutManagement.removeMapping(table, user, domain, mapping);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    protected String getMappingValue(String raw) {
        if (raw.equals("*")) {
            return null;
        } else {
            return raw;
        }
    }
}
