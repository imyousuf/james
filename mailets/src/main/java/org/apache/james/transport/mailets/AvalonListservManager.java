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



package org.apache.james.transport.mailets;

import javax.annotation.Resource;

import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersStore;
import org.apache.mailet.MailAddress;

/**
 * Adds or removes an email address to a listserv.
 *
 * <p>Sample configuration:
 * <br>&lt;mailet match="CommandForListserv=james@list.working-dogs.com" class="AvalonListservManager"&gt;
 * <br>&lt;repositoryName&gt;name of user repository configured in UsersStore block &lt;/repositoryName&gt;
 * <br>&lt;/mailet&gt;
 *
 * @version This is $Revision$
 */
public class AvalonListservManager extends GenericListservManager {

    private UsersRepository members;

    private UsersStore usersStore;

    @Resource(name="usersstore")
    public void setUsersStore(UsersStore usersStore) {
        this.usersStore = usersStore;
    }
    
    /**
     * Initialize the mailet
     */
    public void init() {
        try {
            String repName = getInitParameter("repositoryName");

            members = (UsersRepository) usersStore.getRepository(repName);
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }
    }

    /**
     * Add an address to the list.
     *
     * @param address the address to add
     *
     * @return true if successful, false otherwise
     */
    public boolean addAddress(MailAddress address) {
        members.addUser(address.toString(), "");
        return true;
    }

    /**
     * Remove an address from the list.
     *
     * @param address the address to remove
     *
     * @return true if successful, false otherwise
     */
    public boolean removeAddress(MailAddress address) {
        members.removeUser(address.toString());
        return true;
    }

    public boolean existsAddress(MailAddress address) {
        return members.contains(address.toString());
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "AvalonListservManager Mailet";
    }
}

