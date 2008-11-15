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

package org.apache.james.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;


public class JCRUsersRepository extends AbstractJCRRepository implements UsersRepository {
    
    //TODO: Add namespacing
    private static final String PASSWD_PROPERTY = "passwd";

    private static final String USERNAME_PROPERTY = "username";

    private static final Log LOGGER = LogFactory.getLog(JCRMailRepository.class);
    
    /**
     * For setter injection.
     */    
    public JCRUsersRepository() {
        super(LOGGER);
        this.path = "users";
    }

    /**
     * Maximal constructor for injection.
     * @param repository not null
     * @param credentials login credentials for accessing the repository
     * or null to use default credentials
     * @param workspace name of the workspace used as the mail repository.
     * or null to use default workspace
     * @param path path (relative to root) of the user node within the workspace,
     * or null to use default.
     */
    public JCRUsersRepository(Repository repository, Credentials credentials, String workspace, String path, Log logger) {
        super(repository, credentials, workspace, path, logger);
    }
    
    /**
     * Minimal constructor for injection.
     * @param repository not null
     */
    public JCRUsersRepository(Repository repository) {
        super(repository, LOGGER);
        this.path = "users";
    }

    /**
     * Adds a user to the repository with the specified User object.
     *
     * @param user the user to be added
     *
     * @return true if succesful, false otherwise
     * @since James 1.2.2
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
     * @since James 2.3.0
     */
    public boolean addUser(String username, String password) {

        try {
            final Session session = login();
            try {
                final String name = toSafeName(username);
                final String path = this.path + "/" + name;
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
                    parent = rootNode.getNode(this.path);
                } catch (PathNotFoundException e) {
                    // TODO: Need to consider whether should insist that parent
                    // TODO: path exists.
                    parent = rootNode.addNode(this.path);
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

    /**
     * Get the user object with the specified user name.  Return null if no
     * such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     */
    public User getUserByName(String username) {
        User user;
        try {
            final Session session = login();
            try {
                final String name = toSafeName(username);
                final String path = this.path + "/" + name;
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
                    final String path = this.path + "/" + name;
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
                final String path = this.path + "/" + name;
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
                final String path = this.path + "/" + toSafeName(name);                
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
                final String path = this.path + "/" + name;
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
                    final Node node = rootNode.getNode(path);
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
    public Iterator list() {
        final Collection userNames = new ArrayList();
        try {
            final Session session = login();
            try {
                final Node rootNode = session.getRootNode();
                try {
                    final Node baseNode = rootNode.getNode(path);
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
}
