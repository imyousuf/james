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

package org.apache.james.remotemanager;

import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.rmi.RemoteException;
import javax.mail.internet.ParseException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.services.JamesUser;
import org.apache.mailet.MailAddress;


/**
 * This class do all the hard work to manage the user inside James.
 * 
 * @author <a href="buchi@email.com">Gabriel Bucher</a>
 */
public class UserManagerImpl extends AbstractLogEnabled
        implements UserManager, Contextualizable, Composable, Configurable, Initializable {

    private MailServer mailServer;
    private UsersStore usersStore;
    private UsersRepository users;


    public void contextualize(Context context)
            throws ContextException {
    }

    public void compose(ComponentManager componentManager)
            throws ComponentException {
        this.mailServer = (MailServer)componentManager.lookup( "org.apache.james.services.MailServer" );
        this.usersStore = (UsersStore)componentManager.lookup( "org.apache.james.services.UsersStore" );
        this.users = usersStore.getRepository("LocalUsers");
    }

    public void configure(Configuration configuration)
            throws ConfigurationException {
    }

    public void initialize()
            throws Exception {
    }



    public ArrayList getRepositoryNames()
            throws RemoteException {
        Iterator iterator = usersStore.getRepositoryNames();
        ArrayList list = new ArrayList();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    public boolean setRepository(String name)
            throws RemoteException {
        UsersRepository repos = usersStore.getRepository(name);
        if (repos == null) {
            getLogger().warn("No user repository found with name " + name);
            return false;
        }
        this.users = repos;
        getLogger().info("Set to new user repository " + name);
        return true;
    }

    public boolean addUser(String username, 
                           String password) 
            throws RemoteException {
        if (users.contains(username)) {
            getLogger().error("User " + username + " already exist!");
            throw new RemoteException("User " + username + " already exist!");
        }
        if (mailServer.addUser(username, password)) {
            getLogger().info("User " + username + " successful added.");
            return true;
        }
        getLogger().error("Error adding user " + username);
        return false;
    }

    public boolean deleteUser(String username) 
            throws RemoteException {
        try {
            users.removeUser(username);
            getLogger().info("User " + username + " successful deleted.");
            return true;
        } catch (Exception e) {
            getLogger().error("Error deleting user " + username + " - " + e.getMessage());
        }
        return false;
    }

    public boolean verifyUser(String username) 
            throws RemoteException {
        return users.contains(username);
    }

    public int getCountUsers() 
            throws RemoteException {
        return users.countUsers();
    }

    public ArrayList getUserList() 
            throws RemoteException {
        Iterator iterator = users.list();
        ArrayList list = new ArrayList();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    public boolean setPassword(String username, 
                               String password) 
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        if (user.setPassword(password)) {
            users.updateUser(user);
            getLogger().info("Password for user " + username + " reset.");
            return true;
        }
        getLogger().error("Error resetting password for user " + username);
        return false;
    }


    public boolean setAlias(String username, 
                            String alias) 
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        JamesUser aliasUser = (JamesUser)users.getUserByName(alias);
        if (aliasUser == null) {
            getLogger().error("Alias unknown to server - create that user " + alias + " first!");
            throw new RemoteException("Alias unknown to server - create that user " + alias + " first!");
        }
        if (user.setAlias(alias)) {
            user.setAliasing(true);
            users.updateUser(user);
            getLogger().info("Alias for user " + username + " set to: " + alias);
            return true;
        }
        getLogger().error("Error setting alias " + alias + " for user " + username);
        return false;
    }


    public boolean unsetAlias(String username)
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        if (user.getAliasing()) {
            user.setAliasing(false);
            users.updateUser(user);
            getLogger().info("Alias for user " + username + " unset.");
            return true;
        }
        getLogger().info("Aliasing not active for user " + username);
        return false;
    }

    public String checkAlias(String username)
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        if (user.getAliasing()) {
            String alias = user.getAlias();
            getLogger().info("Alias is set to " + alias + " for user " + username);
            return alias;
        }
        getLogger().info("No alias is set for this user " + username);
        return null;
    }


    public boolean setForward(String username, 
                              String forward) 
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        MailAddress forwardAddress;
        try {
            forwardAddress = new MailAddress(forward);
        } catch (ParseException pe) {
            getLogger().error("Parse exception with that email address: " + pe.getMessage());
            throw new RemoteException("Parse exception with that email address: " + pe.getMessage());
        }
        if (user.setForwardingDestination(forwardAddress)) {
            user.setForwarding(true);
            users.updateUser(user);
            getLogger().info("Forwarding destination for " + username + " set to: " + forwardAddress.toString());
            return true;
        }
        getLogger().error("Error setting forward for user " + username);
        return false;
    }

    public boolean unsetForward(String username) 
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        if (user.getForwarding()) {
            user.setForwarding(false);
            users.updateUser(user);
            getLogger().info("Forward for user " + username + " unset.");
            return true;
        }
        getLogger().info("Forwarding not active for user " + username);
        return false;
    }

    public String checkForward(String username) 
            throws RemoteException {
        JamesUser user = (JamesUser)users.getUserByName(username);
        if (user == null) {
            getLogger().error("No such user " + username + " found!");
            throw new RemoteException("No such user " + username + " found!");
        }
        if (user.getForwarding()) {
            String forward = user.getForwardingDestination().toString();
            getLogger().info("Forward is set to " + forward + " for user " + username);
            return forward;
        }
        getLogger().info("No forward set for user " + username);
        return null;
    }

}
