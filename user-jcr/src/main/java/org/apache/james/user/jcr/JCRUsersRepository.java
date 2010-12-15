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

package org.apache.james.user.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.user.api.User;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.jcr.model.JCRUser;

/**
 * {@link UsersRepository} implementation which stores users to a JCR {@link Repository}
 *
 */
public class JCRUsersRepository implements UsersRepository, Configurable, LogEnabled {
    
    //TODO: Add namespacing
    private static final String PASSWD_PROPERTY = "passwd";

    private static final String USERNAME_PROPERTY = "username";
    private static final String USERS_PATH = "users";

	private Repository repository;
	private SimpleCredentials creds;
	private String workspace;

	private Log logger;

    private boolean virtualHosting;
	
    @Resource(name="jcrRepository")
    public void setRepository(Repository repository) {
    	this.repository = repository;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
	public void configure(HierarchicalConfiguration config)
			throws ConfigurationException {
		this.workspace = config.getString("workspace",null);
		String username = config.getString("username", null);
		String password = config.getString("password",null);
		
		if (username != null && password != null) {
			this.creds = new SimpleCredentials(username, password.toCharArray());
		}
        virtualHosting = config.getBoolean("enableVirtualHosting", false);

	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
	 */
	public void setLog(Log log) {		
		this.logger = log;
	}

    /**
     * Adds a user to the repository with the specified User object.
     *
     * @param user the user to be added
     *
     * @return true if succesful, false otherwise
     * 
     * @deprecated James 2.4 user should be added using username/password
     * because specific implementations of UsersRepository will support specific 
     * implementations of users object.
     */
    public boolean addUser(User user) {
        throw new UnsupportedOperationException("Unsupported by JCR");
    }

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     *
     * @param name the name of the user to be added
     * @param attributes see decription
     * 
     * @deprecated James 2.4 user is always added using username/password and
     * eventually modified by retrieving it later.
     */
    public void addUser(String name, Object attributes) {
        if (attributes instanceof String) {
            addUser(name, (String) attributes);
        } else {
            throw new IllegalArgumentException("Expected password string");
        }
    }
    
    /**
     * Adds a user to the repository with the specified password
     * 
     * @param username the username of the user to be added
     * @param password the password of the user to add
     * @return true if succesful, false otherwise
     * 
     */
    public boolean addUser(String username, String password) {

        try {
            final Session session = login();
            try {
                final String name = toSafeName(username);
                final String path = USERS_PATH + "/" + name;
                final Node rootNode = session.getRootNode();
                try {
                    rootNode.getNode(path);
                    logger.info("User already exists");
                    return false;
                } catch (PathNotFoundException e) {
                    // user does not exist
                }
                Node parent;
                try {
                    parent = rootNode.getNode(USERS_PATH);
                } catch (PathNotFoundException e) {
                    // TODO: Need to consider whether should insist that parent
                    // TODO: path exists.
                    parent = rootNode.addNode(USERS_PATH);
                }
                
                Node node = parent.addNode(name);
                node.setProperty(USERNAME_PROPERTY, username);
                final String hashedPassword;
                if (password == null)
                {
                    // Support easy password reset
                    hashedPassword = "";
                }
                else
                {
                    hashedPassword = JCRUser.hashPassword(username, password);
                }
                node.setProperty(PASSWD_PROPERTY, hashedPassword);
                session.save();
                return true;
            } finally {
                session.logout();
            }
            
        } catch (RepositoryException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to add user: " + username, e);
            }
        }

        return false;
    }
    
    protected String toSafeName(String key) {
        String name = ISO9075.encode(Text.escapeIllegalJcrChars(key));
        return name;
    }
    
    private Session login() throws RepositoryException{
    	return repository.login(creds, workspace);
    }
    
    /**
     * Get the user object with the specified user name.  Return null if no
     * such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     */
    public User getUserByName(String username) {
        User user;
        try {
            final Session session = login();
            try {
                final String name = toSafeName(username);
                final String path = USERS_PATH + "/" + name;
                final Node rootNode = session.getRootNode();
                
                try {
                    final Node node = rootNode.getNode(path);
                    user = new JCRUser(node.getProperty(USERNAME_PROPERTY).getString(), 
                            node.getProperty(PASSWD_PROPERTY).getString());
                } catch (PathNotFoundException e) {
                    // user not found
                    user = null;
                }
            } finally {
                session.logout();
            }
            
        } catch (RepositoryException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to add user: " + username, e);
            }
            user = null;
        }
        return user;
    }

    /**
     * Get the user object with the specified user name. Match user naems on
     * a case insensitive basis.  Return null if no such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     * @deprecated James 2.4 now caseSensitive is a property of the repository
     * implementations and the getUserByName will search according to this property.
     */
    public User getUserByNameCaseInsensitive(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the user name of the user matching name on an equalsIgnoreCase
     * basis. Returns null if no match.
     *
     * @param name the name to case-correct
     * @return the case-correct name of the user, null if the user doesn't exist
     */
    public String getRealName(String name) {
        return null;
    }

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @return true if successful.
     */
    public boolean updateUser(final User user) {
        if (user != null && user instanceof JCRUser)
        {
            final JCRUser jcrUser = (JCRUser) user;
            final String userName = jcrUser.getUserName();
            try {
                final Session session = login();
                try {
                    final String name = toSafeName(userName);
                    final String path = USERS_PATH + "/" + name;
                    final Node rootNode = session.getRootNode();
                    
                    try {
                        final String hashedSaltedPassword = jcrUser.getHashedSaltedPassword();
                        rootNode.getNode(path).setProperty(PASSWD_PROPERTY, hashedSaltedPassword);
                        session.save();
                        return true;
                    } catch (PathNotFoundException e) {
                        // user not found
                        logger.debug("User not found");
                    }
                } finally {
                    session.logout();
                }
                
            } catch (RepositoryException e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Failed to add user: " + userName, e);
                }
            }
        }
        return false;
    }

    /**
     * Removes a user from the repository
     *
     * @param name the user to remove from the repository
     */
    public void removeUser(String username) {
        try {
            final Session session = login();
            try {
                final String name = toSafeName(username);
                final String path = USERS_PATH + "/" + name;
                try {
                    session.getRootNode().getNode(path).remove();
                    session.save();
                } catch (PathNotFoundException e) {
                    // user not found
                }
            } finally {
                session.logout();
            }
            
        } catch (RepositoryException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to add user: " + username, e);
            }
        }
    }

    /**
     * Returns whether or not this user is in the repository
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     */
    public boolean contains(String name) {
        try {
            final Session session = login();
            try {
                final Node rootNode = session.getRootNode();
                final String path = USERS_PATH + "/" + toSafeName(name);                
                rootNode.getNode(path);
                return true;
            } finally {
                session.logout();
            }
            
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("User not found: " + name, e);
            }
        }

        return false;
    }

    /**
     * Returns whether or not this user is in the repository. Names are
     * matched on a case insensitive basis.
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     * 
     * @deprecated James 2.4 now caseSensitive is a property of the repository
     * implementations and the contains will search according to this property.
     */
    public boolean containsCaseInsensitive(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Test if user with name 'name' has password 'password'.
     *
     * @param name the name of the user to be tested
     * @param password the password to be tested
     *
     * @return true if the test is successful, false if the user
     *              doesn't exist or if the password is incorrect
     *
     * @since James 1.2.2
     */
    public boolean test(String username, String password) {
        try {
            final Session session = login();
            try {
                final String name = toSafeName(username);
                final String path = USERS_PATH + "/" + name;
                final Node rootNode = session.getRootNode();
                
                try {
                    final Node node = rootNode.getNode(path);
                    final String current = node.getProperty(PASSWD_PROPERTY).getString();
                    if (current == null || current == "")
                    {
                        return password == null || password == "";
                    }
                    final String hashPassword = JCRUser.hashPassword(username, password);
                    return current.equals(hashPassword);
                } catch (PathNotFoundException e) {
                    // user not found
                    logger.debug("User not found");
                    return false;
                }
            } finally {
                session.logout();
            }
            
        } catch (RepositoryException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to add user: " + username, e);
            }
            return false;
        }

    }

    /**
     * Returns a count of the users in the repository.
     *
     * @return the number of users in the repository
     */
    public int countUsers() {
        try {
            final Session session = login();
            try {
                final Node rootNode = session.getRootNode();
                try {
                    final Node node = rootNode.getNode(USERS_PATH);
                    //TODO: Use query
                    //TODO: Use namespacing to avoid unwanted nodes
                    NodeIterator it = node.getNodes();
                    return (int) it.getSize();
                } catch (PathNotFoundException e) {
                    return 0;
                }
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to count user", e);
            }
            return 0;
        }
    }

    /**
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    public Iterator<String> list() {
        final Collection<String> userNames = new ArrayList<String>();
        try {
            final Session session = login();
            try {
                final Node rootNode = session.getRootNode();
                try {
                    final Node baseNode = rootNode.getNode(USERS_PATH);
                    //TODO: Use query
                    final NodeIterator it = baseNode.getNodes();
                    while(it.hasNext()) {
                        final Node node = it.nextNode();
                        try {
                            final String userName = node.getProperty(USERNAME_PROPERTY).getString();
                            userNames.add(userName);
                        } catch (PathNotFoundException e) {
                            logger.info("Node missing user name. Ignoring.");
                        }
                    }
                } catch (PathNotFoundException e) {
                    logger.info("Path not found. Forgotten to setup the repository?");
                }
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to count user", e);
            }
        }
        return userNames.iterator();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UsersRepository#supportVirtualHosting()
     */
    public boolean supportVirtualHosting() {
        return virtualHosting;
    }

}
