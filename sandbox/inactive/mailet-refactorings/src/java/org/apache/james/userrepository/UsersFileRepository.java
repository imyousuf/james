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

import org.apache.avalon.cornerstone.services.store.ObjectRepository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.mailet.User;

import java.util.Iterator;

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
 * @version CVS $Revision$
 *
 */
public class UsersFileRepository
    extends AbstractUsersRepository
    implements Configurable, Serviceable, Initializable {
 
    /**
     * Whether 'deep debugging' is turned on.
     */
    protected static boolean DEEP_DEBUG = false;

    private Store store;
    private ObjectRepository objectRepository;
    private static String urlSeparator = "/"; 

    /**
     * The destination URL used to define the repository.
     */
    private String destination;

    /**
     * Set the Store
     * 
     * @param store the Store
     */
    public void setStore(Store store) {
        this.store = store;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {

        try {
            setStore((Store)componentManager.lookup( Store.ROLE ));
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error( message, e );
            throw new ServiceException ("", message, e );
        }
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {
        super.configure(configuration);
        destination = configuration.getChild( "destination" ).getAttribute( "URL" );

        if (!destination.endsWith(urlSeparator)) {
            destination += urlSeparator;
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
        throws Exception {

        try {
            //prepare Configurations for object and stream repositories
            final DefaultConfiguration objectConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:UsersFileRepository.compose()" );

            objectConfiguration.setAttribute( "destinationURL", destination );
            objectConfiguration.setAttribute( "type", "OBJECT" );
            objectConfiguration.setAttribute( "model", "SYNCHRONOUS" );

            objectRepository = (ObjectRepository)store.select( objectConfiguration );
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
     * @see org.apache.mailet.UsersRepository#list()
     */
    public Iterator list() {
        return objectRepository.list();
    }

    /**
     * @see org.apache.james.userrepository.AbstractUsersRepository#doAddUser(org.apache.mailet.User)
     */
    protected void doAddUser(User user) {
        try {
            objectRepository.put(user.getUserName(), user);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: " + e );
        }
    }

    /**
     * @see org.apache.mailet.UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String username, String password) {
        User newbie = new DefaultJamesUser(username, "SHA");
        newbie.setPassword(password);
        return addUser(newbie);
    }

    /**
     * @see org.apache.mailet.UsersRepository#getUserByName(java.lang.String)
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
     * @see org.apache.mailet.UsersRepository#getUserByNameCaseInsensitive(java.lang.String)
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
            Iterator it = list();
            while (it.hasNext()) {
                String temp = (String) it.next();
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
     * @see org.apache.mailet.UsersRepository#getRealName(java.lang.String)
     */
    public String getRealName(String name) {
        return getRealName(name, ignoreCase);
    }
    
    /**
     * @see org.apache.james.userrepository.AbstractUsersRepository#doUpdateUser(org.apache.mailet.User)
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
     * @see org.apache.mailet.UsersRepository#removeUser(java.lang.String)
     */
    public synchronized void removeUser(String name) {
        objectRepository.remove(name);
    }

    /**
     * @see org.apache.mailet.UsersRepository#contains(java.lang.String)
     */
    public boolean contains(String name) {
        if (ignoreCase) {
            return containsCaseInsensitive(name);
        } else {
            return objectRepository.containsKey(name);
        }
    }

    /**
     * @see org.apache.mailet.UsersRepository#containsCaseInsensitive(java.lang.String)
     */
    public boolean containsCaseInsensitive(String name) {
        Iterator it = list();
        while (it.hasNext()) {
            if (name.equalsIgnoreCase((String)it.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.mailet.UsersRepository#test(java.lang.String, java.lang.String)
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
     * @see org.apache.mailet.UsersRepository#countUsers()
     */
    public int countUsers() {
        int count = 0;
        for (Iterator it = list(); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

}
