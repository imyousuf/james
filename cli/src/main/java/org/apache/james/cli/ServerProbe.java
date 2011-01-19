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
package org.apache.james.cli;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.james.domainlist.api.DomainListManagementMBean;
import org.apache.james.user.api.UsersRepositoryManagementMBean;
import org.apache.james.vut.api.VirtualUserTableManagementMBean;

public class ServerProbe {

    //TODO: Move this to somewhere else
    private final static String DOMAINLIST_OBJECT_NAME = "org.apache.james:type=component,name=domainlist";
    private final static String VIRTUALUSERTABLE_OBJECT_NAME = "org.apache.james:type=component,name=virtualusertable";
    private final static String USERSREPOSITORY_OBJECT_NAME = "org.apache.james:type=component,name=usersrepository";
    /**
     * Create a connection to the JMX agent and setup the M[X]Bean proxies.
     * 
     * @throws IOException on connection failures
     */
    private MBeanServerConnection mbeanServerConn;
    private DomainListManagementMBean domainListProcxy;
    private VirtualUserTableManagementMBean virtualUserTableProxy;
    private UsersRepositoryManagementMBean usersRepositoryProxy;
   
    private static final String fmtUrl = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";
    private static final int defaultPort = 9999;
    private String host;
    private int port;

    /**
     * Creates a ServerProbe using the specified JMX host and port.
     * 
     * @param host hostname or IP address of the JMX agent
     * @param port TCP port of the remote JMX agent
     * @throws IOException on connection failures
     */
    public ServerProbe(String host, int port) throws IOException, InterruptedException
    {
        this.host = host;
        this.port = port;
        connect();
    }
    
    /**
     * Creates a NodeProbe using the specified JMX host and default port.
     * 
     * @param host hostname or IP address of the JMX agent
     * @throws IOException on connection failures
     */
    public ServerProbe(String host) throws IOException, InterruptedException
    {
        this.host = host;
        this.port = defaultPort;
        connect();
    }
    

    
    private void connect() throws IOException
    {
        JMXServiceURL jmxUrl = new JMXServiceURL(String.format(fmtUrl, host, port));
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, null);
        mbeanServerConn = jmxc.getMBeanServerConnection();
        
        try
        {
            ObjectName name = new ObjectName(DOMAINLIST_OBJECT_NAME);
            domainListProcxy = (DomainListManagementMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServerConn, name, DomainListManagementMBean.class, true);
            name = new ObjectName(VIRTUALUSERTABLE_OBJECT_NAME);
            virtualUserTableProxy = (VirtualUserTableManagementMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServerConn, name, VirtualUserTableManagementMBean.class, true);
            name = new ObjectName(USERSREPOSITORY_OBJECT_NAME);
            usersRepositoryProxy = (UsersRepositoryManagementMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServerConn, name, UsersRepositoryManagementMBean.class, true);
        } catch (MalformedObjectNameException e)
        {
            throw new RuntimeException(
                    "Invalid ObjectName? Please report this as a bug.", e);
        }
    }
    
    public void addUser(String userName, String password) throws Exception {
        usersRepositoryProxy.addUser(userName, password);
    }

    public void removeUser(String username) throws Exception {
        usersRepositoryProxy.deleteUser(username);
    }
    
    public String[] listUsers() throws Exception {
        return usersRepositoryProxy.listAllUsers();
    }
    
    public void setPassword(String userName, String password) throws Exception {
        usersRepositoryProxy.setPassword(userName, password);
    }

    public void addDomain(String domain) throws Exception {
        domainListProcxy.addDomain(domain);
    }
    
    public void removeDomain(String domain) throws Exception {
        domainListProcxy.removeDomain(domain);
    }
    
    public String[] listDomains() throws Exception {
        return domainListProcxy.getDomains();
    }
    
    public Map<String, Collection<String>> listMappings() throws Exception {
        return virtualUserTableProxy.getAllMappings();
    }
    
    public void addAddressMapping(String user, String domain, String toAddress) throws Exception {
        virtualUserTableProxy.addAddressMapping(user, domain, toAddress);
    }
    
    public void removeAddressMapping(String user, String domain, String fromAddress) throws Exception {
        virtualUserTableProxy.removeAddressMapping(user, domain, fromAddress);
    }
    
    public Collection<String> listUserDomainMappings(String user, String domain) throws Exception {
        return virtualUserTableProxy.getUserDomainMappings(user, domain);
    }
    
    public void addRegexMapping(String user, String domain, String regex) throws Exception {
        virtualUserTableProxy.addRegexMapping(user, domain, regex);
    }
    
    public void removeRegexMapping(String user, String domain, String regex) throws Exception {
        virtualUserTableProxy.removeRegexMapping(user, domain, regex);
    }
}
