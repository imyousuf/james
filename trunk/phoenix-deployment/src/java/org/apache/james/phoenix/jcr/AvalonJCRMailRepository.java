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
package org.apache.james.phoenix.jcr;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.rmi.jackrabbit.JackrabbitClientAdapterFactory;
import org.apache.james.jcr.JCRMailRepository;

/**
 * Managed Avalon wrapper for the {@link JCRMailRepository} class.
 */
public class AvalonJCRMailRepository extends JCRMailRepository
        implements Configurable, Initializable {

    //--------------------------------------------------------< Configurable >

    public void configure(Configuration configuration)
            throws ConfigurationException {
        String repository = configuration.getChild("repository").getValue();
        try {
            ClientRepositoryFactory factory = new ClientRepositoryFactory(
                    new JackrabbitClientAdapterFactory());
            setRepository(factory.getRepository(repository));
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Invalid repository address: " + repository, e);
        }

        String username = configuration.getChild("username").getValue(null);
        String password = configuration.getChild("password").getValue(null);
        if (username != null && password != null) {
            setCredentials(
                    new SimpleCredentials(username, password.toCharArray()));
        }

        String workspace = configuration.getChild("workspace").getValue(null);
        if (workspace != null) {
            setWorkspace(workspace);
        }

        String path =
            configuration.getAttribute("destinationURL", "james:repository");
        int i = path.indexOf("://");
        if (i != -1) {
            path = path.substring(i + "://".length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        setPath(path);
    }

    //-------------------------------------------------------< Initializable >

    public void initialize() throws Exception {
        Session session =
            getRepository().login(getCredentials(), getWorkspace());
        try {
            NodeTypeManager manager =
                session.getWorkspace().getNodeTypeManager();
            if (manager instanceof JackrabbitNodeTypeManager) {
                JackrabbitNodeTypeManager jackrabbit =
                    (JackrabbitNodeTypeManager) manager;
                if (!jackrabbit.hasNodeType("james:mail")) {
                    Class<?> clazz = AvalonJCRMailRepository.class; 
                    jackrabbit.registerNodeTypes(
                            clazz.getResourceAsStream("james.cnd"),
                            JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
                }
            }

            if (!session.getRootNode().hasNode(getPath())) {
                session.getRootNode().addNode(getPath(), "nt:folder");
                session.save();
            }
        } finally {
            session.logout();
        }
    }

}
