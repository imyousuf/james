/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.james.jcr;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.mail.MessagingException;

/**
 * Mailet that stores messages to a JCR content repository.
 */
public class JCRStoreMailet implements Mailet {

    /**
     * Mailet configuration.
     */
    private MailetConfig config;

    /**
     * JCR content repository.
     */
    private Repository repository;

    /**
     * Returns information about this mailet.
     *
     * @return mailet information
     */
    public String getMailetInfo() {
        return "JCR Store Mailet";
    }

    /**
     * Returns the mailet configuration.
     *
     * @return mailet configuration
     */
    public MailetConfig getMailetConfig() {
        return config;
    }

    /**
     * Initializes this mailet by connecting to the configured JCR repository.
     *
     * @param config mailet configuration
     * @throws MessagingException if the JCR repository can not be accessed
     */
    public void init(MailetConfig config) throws MessagingException {
        this.config = config;

        String url = config.getInitParameter("url");
        try {
            ClientRepositoryFactory factory = new ClientRepositoryFactory();
            this.repository = factory.getRepository(url);
        } catch (Exception e) {
            throw new MessagingException(
                    "Error accessing the content repository: " + url, e);
        }
    }

    /**
     * Closes this mailet by releasing the JCR connection.
     */
    public void destroy() {
        this.repository = null;
        this.config = null;
    }

    /**
     * Stores the given mail message to the content repository.
     *
     * @param mail mail message
     * @throws MessagingException if the message could not be saved
     */
    public void service(Mail mail) throws MessagingException {
        try {
            String username = config.getInitParameter("username");
            String password = config.getInitParameter("password");
            String workspace = config.getInitParameter("workspace");
            String path = config.getInitParameter("path");

            Credentials credentials = null;
            if (username != null) {
                credentials =
                    new SimpleCredentials(username, password.toCharArray());
            }

            Session session = repository.login(credentials, workspace);
            try {
                Item item = session.getItem(path);
                if (item instanceof Node) {
                    JCRStoreBean bean = new JCRStoreBean();
                    bean.setParentNode((Node) item);
                    bean.storeMessage(mail.getMessage());
                } else {
                    throw new MessagingException("Invalid path: " + path);
                }
            } finally {
                session.logout();
            }
//        } catch (IOException e) {
//            throw new MessagingException("IO error", e);
        } catch (RepositoryException e) {
            throw new MessagingException("Repository access error", e);
        }
    }

}
