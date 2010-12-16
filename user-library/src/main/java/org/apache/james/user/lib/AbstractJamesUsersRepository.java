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



package org.apache.james.user.lib;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.user.api.JamesUsersRepository;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.JamesUser;
import org.apache.james.user.api.model.User;
import org.apache.james.user.lib.model.DefaultUser;
import org.apache.james.vut.lib.AbstractReadOnlyVirtualUserTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * A partial implementation of a Repository to store users.
 * <p>This implements common functionality found in different UsersRespository 
 * implementations, and makes it easier to create new User repositories.</p>
 *
 *@deprecated Please implement {@link UsersRepository}
 */
@Deprecated
public abstract class AbstractJamesUsersRepository extends AbstractReadOnlyVirtualUserTable implements JamesUsersRepository, LogEnabled, Configurable {

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


    private Log logger;

    private boolean virtualHosting;

    public void setLog(Log logger) {
        this.logger = logger;
    }
    
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException{
        setIgnoreCase(configuration.getBoolean("ignoreCase", false));
        setEnableAliases(configuration.getBoolean("enableAliases", false));
        setEnableForwarding(configuration.getBoolean("enableForwarding", false));
        virtualHosting = configuration.getBoolean("enableVirtualHosting", false);

        doConfigure(configuration);
    }
    
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException{
        
    }

    public void setEnableVirtualHosting(boolean virtualHosting) {
        this.virtualHosting = virtualHosting;
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
     * @see org.apache.james.vut.api.VirtualUserTable#getMappings(java.lang.String,
     *      java.lang.String)
     */
    public Collection<String> getMappings(String username, String domain)
            throws ErrorMappingException {
        Collection<String> mappings = new ArrayList<String>();
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

    protected Log getLogger() {
        return logger;
    }
    
    
    
    /**
     * @see org.apache.james.user.api.JamesUsersRepository#setEnableAliases(boolean)
     */
    public void setEnableAliases(boolean enableAliases) {
        this.enableAliases = enableAliases;
    }

    /**
     * @see org.apache.james.user.api.JamesUsersRepository#setEnableForwarding(boolean)
     */
    public void setEnableForwarding(boolean enableForwarding) {
        this.enableForwarding = enableForwarding;
    }

    /**
     * @see org.apache.james.user.api.JamesUsersRepository#setIgnoreCase(boolean)
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.vut.api.VirtualUserTable#getAllMappings()
     */
    public Map<String, Collection<String>> getAllMappings() {
        Map<String, Collection<String>> mappings = new HashMap<String, Collection<String>>();
        if (enableAliases == true || enableForwarding == true) {
            Iterator<String> users = list();
            while(users.hasNext()) {
                String user = users.next();
                int index = user.indexOf("@");
                String username;
                String domain;
                if (index != -1) {
                    username = user.substring(0, index);
                    domain = user.substring(index +1, user.length());
                } else {
                    username = user;
                    domain = "localhost";
                }
                try {
                    mappings.put(user, getMappings(username, domain));
                } catch (ErrorMappingException e) {
                    // shold never happen here
                }
            }        
        }
       
        return mappings;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.vut.api.VirtualUserTable#getUserDomainMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getUserDomainMappings(String user, String domain) {
        return new ArrayList<String>();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UsersRepository#supportVirtualHosting()
     */
    public boolean supportVirtualHosting() {
        return virtualHosting;
    }

    
}
