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



package org.apache.james.userrepository;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.JamesUser;
import org.apache.james.services.JamesUsersRepository;
import org.apache.james.services.User;
import org.apache.james.vut.ErrorMappingException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A partial implementation of a Repository to store users.
 * <p>This implements common functionality found in different UsersRespository 
 * implementations, and makes it easier to create new User repositories.</p>
 *
 */
public abstract class AbstractUsersRepository
    extends AbstractLogEnabled
    implements Configurable, JamesUsersRepository {

    /**
     * Ignore case in usernames
     */
    protected boolean ignoreCase;
    
    /**
     * Enable Aliases frmo JamesUser
     */
    protected boolean enableAliases;
    
    /**
     * Wether to enable forwarding for JamesUser or not
     */
    protected boolean enableForwarding;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration configuration)
        throws ConfigurationException {
        setIgnoreCase(configuration.getChild("usernames").getValueAsBoolean(false));
        setEnableAliases(configuration.getChild("enableAliases").getValueAsBoolean(false));
        setEnableForwarding(configuration.getChild("enableForwarding").getValueAsBoolean(false));
    }

    /**
     * Adds a user to the underlying Repository. The user name must not clash
     * with an existing user.
     * 
     * @param user
     *            the user to add
     */
    protected abstract void doAddUser(User user);

    /**
     * Updates a user record to match the supplied User.
     * 
     * @param user
     *            the user to update
     */
    protected abstract void doUpdateUser(User user);


    /**
     * Adds a user to the repository with the specified attributes. In current
     * implementations, the Object attributes is generally a String password.
     * 
     * @param name
     *            the name of the user to be added
     * @param attributes
     *            the password value as a String
     */
    public void addUser(String name, Object attributes) {
        if (attributes instanceof String) {
            User newbie = new DefaultUser(name, "SHA");
            newbie.setPassword((String) attributes);
            addUser(newbie);
        } else {
            throw new RuntimeException("Improper use of deprecated method"
                    + " - use addUser(User user)");
        }
    }

    //
    // UsersRepository interface implementation.
    //
    /**
     * Adds a user to the repository with the specified User object. Users names
     * must be unique-case-insensitive in the repository.
     * 
     * @param user
     *            the user to be added
     * 
     * @return true if succesful, false otherwise
     * @since James 1.2.2
     */
    public boolean addUser(User user) {
        String username = user.getUserName();

        if (contains(username)) {
            return false;
        }

        doAddUser(user);
        return true;
    }

    /**
     * Update the repository with the specified user object. A user object with
     * this username must already exist.
     * 
     * @param user
     *            the user to be updated
     * 
     * @return true if successful.
     */
    public boolean updateUser(User user) {
        // Return false if it's not found.
        if (!contains(user.getUserName())) {
            return false;
        } else {
            doUpdateUser(user);
            return true;
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTable#getMappings(java.lang.String,
     *      java.lang.String)
     */
    public Collection getMappings(String username, String domain)
            throws ErrorMappingException {
        Collection mappings = new ArrayList();
        User user = getUserByName(username);

        if (user instanceof JamesUser) {
            JamesUser jUser = (JamesUser) user;

            if (enableAliases && jUser.getAliasing()) {
                String alias = jUser.getAlias();
                if (alias != null) {
                    mappings.add(alias + "@" + domain);
                }
            }

            if (enableForwarding && jUser.getForwarding()) {
                String forward = null;
                if (jUser.getForwardingDestination() != null
                        && ((forward = jUser.getForwardingDestination()
                                .toString()) != null)) {
                    mappings.add(forward);
                } else {
                    StringBuffer errorBuffer = new StringBuffer(128)
                            .append("Forwarding was enabled for ")
                            .append(username)
                            .append(
                                    " but no forwarding address was set for this account.");
                    getLogger().error(errorBuffer.toString());
                }
            }
        }
        if (mappings.size() == 0) {
            return null;
        } else {
            return mappings;
        }
    }

    /**
     * @see org.apache.james.services.JamesUsersRepository#setEnableAliases(boolean)
     */
    public void setEnableAliases(boolean enableAliases) {
        this.enableAliases = enableAliases;
    }

    /**
     * @see org.apache.james.services.JamesUsersRepository#setEnableForwarding(boolean)
     */
    public void setEnableForwarding(boolean enableForwarding) {
        this.enableForwarding = enableForwarding;
    }

    /**
     * @see org.apache.james.services.JamesUsersRepository#setIgnoreCase(boolean)
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

}
