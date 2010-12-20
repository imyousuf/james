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



package org.apache.james.user.file;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.repository.file.FilePersistentObjectRepository;
import org.apache.james.user.api.model.User;
import org.apache.james.user.lib.AbstractJamesUsersRepository;
import org.apache.james.user.lib.model.DefaultJamesUser;


import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Implementation of a Repository to store users on the File System.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="USERS"
 *              model="SYNCHRONOUS"/&gt;
 * Requires a logger called UsersRepository.
 *
 *
 * @version CVS $Revision: 521427 $
 *
 */
@Deprecated
public class UsersFileRepository
    extends AbstractJamesUsersRepository {
 
    /**
     * Whether 'deep debugging' is turned on.
     */
    protected static boolean DEEP_DEBUG = false;

    private FilePersistentObjectRepository objectRepository;
    private static String urlSeparator = "/"; 

    /**
     * The destination URL used to define the repository.
     */
    private String destination;

    private FileSystem fs;


    @Resource(name="filesystem")
    public void setFileSystem(FileSystem fs) {
        this.fs = fs;
    }

    /**
     * @see org.apache.james.user.lib.AbstractJamesUsersRepository#doConfigure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    protected void doConfigure( final HierarchicalConfiguration configuration )
        throws ConfigurationException {
        super.doConfigure(configuration);
        destination = configuration.getString( "destination.[@URL]" );

        if (!destination.endsWith(urlSeparator)) {
            destination += urlSeparator;
        }
    }

    @PostConstruct
    public void init()
        throws Exception {
        try {
            //TODO Check how to remove this!
            //prepare Configurations for object and stream repositories
            final DefaultConfigurationBuilder objectConfiguration
                = new DefaultConfigurationBuilder();

            objectConfiguration.addProperty( "[@destinationURL]", destination );
            
            objectRepository = new FilePersistentObjectRepository();
            objectRepository.setLog(getLogger());
            objectRepository.setFileSystem(fs);
            objectRepository.configure(objectConfiguration);
            objectRepository.init();
            if (getLogger().isDebugEnabled()) {
                StringBuffer logBuffer =
                    new StringBuffer(192)
                            .append(this.getClass().getName())
                            .append(" created in ")
                            .append(destination);
                getLogger().debug(logBuffer.toString());
            }
        } catch (Exception e) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Failed to initialize repository:" + e.getMessage(), e );
            }
            throw e;
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#list()
     */
    public Iterator<String> list() {
        return objectRepository.list();
    }

    /**
     * @see org.apache.james.user.lib.AbstractJamesUsersRepository#doAddUser(org.apache.james.user.api.model.User)
     */
    protected void doAddUser(User user) {
        try {
            objectRepository.put(user.getUserName(), user);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: " + e );
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String username, String password) {
        User newbie = new DefaultJamesUser(username, "SHA");
        newbie.setPassword(password);
        return addUser(newbie);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#getUserByName(java.lang.String)
     */
    public synchronized User getUserByName(String name) {
        if (ignoreCase) {
            name = getRealName(name);
            if (name == null ) {
                return null;
            }
        }
        if (contains(name)) {
            try {
                return (User)objectRepository.get(name);
            } catch (Exception e) {
                throw new RuntimeException("Exception while retrieving user: "
                                           + e.getMessage());
            }
        } else {
            return null;
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#getUserByNameCaseInsensitive(java.lang.String)
     */
    public User getUserByNameCaseInsensitive(String name) {
        String realName = getRealName(name, true);
        if (realName == null ) {
            return null;
        }
        return getUserByName(realName);
    }

    /**
     * Return the real name, given the ignoreCase boolean parameter
     */
    public String getRealName(String name, boolean ignoreCase) {
        if (ignoreCase) {
            Iterator<String> it = list();
            while (it.hasNext()) {
                String temp = it.next();
                if (name.equalsIgnoreCase(temp)) {
                    return temp;
                }
            }
            return null;
        } else {
            return objectRepository.containsKey(name) ? name : null;
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#getRealName(java.lang.String)
     */
    public String getRealName(String name) {
        return getRealName(name, ignoreCase);
    }
    
    /**
     * @see org.apache.james.user.lib.AbstractJamesUsersRepository#doUpdateUser(org.apache.james.user.api.model.User)
     */
    public void doUpdateUser(User user) {
        try {
            objectRepository.put(user.getUserName(), user);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: "
                    + e);
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#removeUser(java.lang.String)
     */
    public synchronized void removeUser(String name) {
        objectRepository.remove(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#contains(java.lang.String)
     */
    public boolean contains(String name) {
        if (ignoreCase) {
            return containsCaseInsensitive(name);
        } else {
            return objectRepository.containsKey(name);
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#containsCaseInsensitive(java.lang.String)
     */
    public boolean containsCaseInsensitive(String name) {
        Iterator<String> it = list();
        while (it.hasNext()) {
            if (name.equalsIgnoreCase((String)it.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#test(java.lang.String, java.lang.String)
     */
    public boolean test(String name, String password) {
        User user;
        try {
            user = getUserByName(name);
            if (user == null) return false;
        } catch (Exception e) {
            throw new RuntimeException("Exception retrieving User" + e);
        }
        return user.verifyPassword(password);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#countUsers()
     */
    public int countUsers() {
        int count = 0;
        for (Iterator<String> it = list(); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

}
