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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;

/**
 * Framework for JCR repositories used by James.
 */
class AbstractJCRRepository {
    
    protected Log logger;

    /**
     * JCR content repository used as the mail repository.
     * Must be set before the any mail operations are performed.
     */
    protected Repository repository;
    
    /**
     * Login credentials for accessing the repository.
     * Set to <code>null</code> (the default) to use default credentials.
     */
    protected Credentials credentials;
    
    /**
     * Name of the workspace used as the mail repository.
     * Set to <code>null</code> (the default) to use the default workspace.
     */
    protected String workspace;
    
    /**
     * Path (relative to root) of the mail repository within the workspace.
     */
    protected String path = "james";

    /**
     * For setter injection.
     */
    public AbstractJCRRepository(Log logger) {
        super();
        this.logger = logger;
    }
    
    /**
     * Minimal constructor for injection.
     * @param repository not null
     */
    public AbstractJCRRepository(Repository repository, Log logger) {
        super();
        this.repository = repository;
        credentials = new SimpleCredentials("userid", "".toCharArray());
        this.logger = logger;
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
    public AbstractJCRRepository(Repository repository, Credentials credentials, String workspace, String path, 
            Log logger) {
        super();
        this.repository = repository;
        this.credentials = credentials;
        this.workspace = workspace;
        this.path = path;
        this.logger = logger;
    }

    
    /**
     * Gets the current logger.
     * @return the logger, not null
     */
    public final Log getLogger() {
        return logger;
    }

    /**
     * Sets the current logger.
     * @param logger the logger to set, not null
     */
    public final void setLogger(Log logger) {
        this.logger = logger;
    }

    /**
     * Retuns the JCR content repository used as the mail repository.
     *
     * @return JCR content repository
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Sets the JCR content repository to be used as the mail repository.
     *
     * @param repository JCR content repository
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Returns the login credentials for accessing the repository.
     *
     * @return login credentials,
     *         or <code>null</code> if using the default credentials
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Sets the login credentials for accessing the repository.
     *
     * @param credentials login credentials,
     *                    or <code>null</code> to use the default credentials
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Returns the name of the workspace used as the mail repository.
     *
     * @return workspace name,
     *         or <code>null</code> if using the default workspace
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Sets the name of the workspace used as the mail repository.
     *
     * @param workspace workspace name,
     *                  or <code>null</code> to use the default workspace
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /**
     * Returns the path of the mail repository within the workspace.
     *
     * @return repository path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path of the mail repository within the workspace.
     *
     * @param path repository path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Logs into a new session.
     * @return new session, not null
     * @throws LoginException when login fails
     * @throws NoSuchWorkspaceException when workspace does not exist
     * @throws RepositoryException when access fails
     */
    protected Session login() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        Session session = repository.login(credentials, workspace);
        return session;
    }

    protected String toSafeName(String key) {
        String name = ISO9075.encode(Text.escapeIllegalJcrChars(key));
        return name;
    }

    protected NodeIterator query(Session session, final String xpath) throws RepositoryException, InvalidQueryException {
        QueryManager manager = session.getWorkspace().getQueryManager();
        Query query = manager.createQuery(xpath, Query.XPATH);
        NodeIterator iterator = query.execute().getNodes();
        return iterator;
    }

}