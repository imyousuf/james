/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.userrepository;

import org.apache.avalon.cornerstone.services.store.ObjectRepository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

import java.io.File;
import java.util.Iterator;

/**
 * Implementation of a Repository to store users on the File System.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  <repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="USERS"
 *              model="SYNCHRONOUS"/>
 * Requires a logger called UsersRepository.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 *
 * @version CVS $Revision: 1.10.4.2 $
 *
 */
public class UsersFileRepository
    extends AbstractLogEnabled
    implements UsersRepository, Component, Configurable, Composable, Initializable {
 
    /**
     * Whether 'deep debugging' is turned on.
     */
    protected static boolean DEEP_DEBUG = false;

    /** @deprecated what was this for? */
    private static final String TYPE = "USERS";

    private Store store;
    private ObjectRepository or;

    /**
     * The destination URL used to define the repository.
     */
    private String destination;

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException {

        try {
            store = (Store)componentManager.
                lookup( "org.apache.avalon.cornerstone.services.store.Store" );
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error( message, e );
            throw new ComponentException( message, e );
        }
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        destination = configuration.getChild( "destination" ).getAttribute( "URL" );

        if (!destination.endsWith(File.separator)) {
            destination += File.separator;
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

            or = (ObjectRepository)store.select( objectConfiguration );
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
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    public Iterator list() {
        return or.list();
    }

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @param user the user to be added.
     *
     * @return true if successful.
     */
    public synchronized boolean addUser(User user) {
        String username = user.getUserName();
        if (contains(username)) {
            return false;
        }
        try {
            or.put(username, user);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: " + e );
        }
        return true;
    }

    public void addUser(String name, Object attributes) {
        if (attributes instanceof String) {
            User newbie = new DefaultUser(name, "SHA");
            newbie.setPassword( (String) attributes);
            addUser(newbie);
        }
        else {
            throw new RuntimeException("Improper use of deprecated method" 
                                       + " - use addUser(User user)");
        }
    }

    public synchronized User getUserByName(String name) {
        if (contains(name)) {
            try {
                return (User)or.get(name);
            } catch (Exception e) {
                throw new RuntimeException("Exception while retrieving user: "
                                           + e.getMessage());
            }
        } else {
            return null;
        }
    }

    public User getUserByNameCaseInsensitive(String name) {
        String realName = getRealName(name);
        if (realName == null ) {
            return null;
        }
        return getUserByName(realName);
    }

    public String getRealName(String name) {
        Iterator it = list();
        while (it.hasNext()) {
            String temp = (String) it.next();
            if (name.equalsIgnoreCase(temp)) {
                return temp;
            }
        }
        return null;
    }

    public Object getAttributes(String name) {
        throw new UnsupportedOperationException("Improper use of deprecated method - read javadocs");
    }

    public boolean updateUser(User user) {
        String username = user.getUserName();
        if (!contains(username)) {
            return false;
        }
        try {
            or.put(username, user);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: " + e );
        }
        return true;
    }

    public synchronized void removeUser(String name) {
        or.remove(name);
    }

    public boolean contains(String name) {
        return or.containsKey(name);
    }

    public boolean containsCaseInsensitive(String name) {
        Iterator it = list();
        while (it.hasNext()) {
            if (name.equalsIgnoreCase((String)it.next())) {
                return true;
            }
        }
        return false;
    }

    public boolean test(String name, Object attributes) {
        try {
            return attributes.equals(or.get(name));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean test(String name, String password) {
        User user;
        try {
            if (contains(name)) {
                user = (User) or.get(name);
            } else {
               return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception retrieving User" + e);
        }
        return user.verifyPassword(password);
    }

    public int countUsers() {
        int count = 0;
        for (Iterator it = list(); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

}
