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

package org.apache.james.mailboxmanager.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.services.User;

public class VirtualMailboxManagerFactory extends AbstractLogEnabled implements
        Configurable, MailboxManagerFactory, Serviceable, Initializable {

    private Map mountMap = null;
    private Collection mailboxManagerFactories = new LinkedList();


    public void configure(Configuration conf) throws ConfigurationException {
        Configuration[] mountsConfs = conf.getChild(
                "mounts", false).getChildren("mount");
        
        for (int i = 0; i < mountsConfs.length; i++) {
            addMountConfig(mountsConfs[i]);
        }        
    }

    public void initialize() throws Exception {
        for (Iterator iter = mailboxManagerFactories.iterator(); iter.hasNext();) {
            ContainerUtil.initialize(iter.next());
        }
    }

    void addMountConfig(Configuration conf) throws ConfigurationException {
        String className = null;
        try {
            Configuration targetConf = conf.getChild("target");
            className = targetConf.getAttribute("class").toString();
            MailboxManagerFactory mailboxManagerFactory = (MailboxManagerFactory) getClassInstace(className);
            mailboxManagerFactories.add(mailboxManagerFactory);
            ContainerUtil.enableLogging(mailboxManagerFactory, getLogger()
                    .getChildLogger(
                            mailboxManagerFactory.getClass().getName()));
            ContainerUtil.configure(mailboxManagerFactory, targetConf);
            
            Configuration[] pointConfs = conf.getChildren("point");
            for (int i = 0; i < pointConfs.length; i++) {
                String point = pointConfs[i].getAttribute("point");
                mailboxManagerFactory.addMountPoint(point);
                addMount(point, mailboxManagerFactory);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Error configure " + className, e);
        }
    }

    private static Object getClassInstace(String name) {
        Object object = null;
        try {
            Class clazz = Thread.currentThread().getContextClassLoader()
                    .loadClass(name);
            object = clazz.newInstance();
            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void deleteEverything() throws MailboxManagerException {
        // TODO Auto-generated method stub

    }

    public MailboxManager getMailboxManagerInstance(User user)

    throws MailboxManagerException {
        VirtualMailboxManager virtualMailboxManager = new VirtualMailboxManager();
        ContainerUtil.enableLogging(virtualMailboxManager, getLogger()
                .getChildLogger(
                        virtualMailboxManager.getClass().getName()));
        virtualMailboxManager.setMountMap(mountMap);
        virtualMailboxManager.setUser(user);

        return virtualMailboxManager;
    }

    public void addMount(String point, MailboxManagerFactory mailboxManager) {
        getMountMap().put(point, mailboxManager);
    }

    protected Map getMountMap() {
        if (mountMap == null) {
            mountMap = new TreeMap(new LengthComparator());
        }
        return mountMap;
    }

    static class LengthComparator implements Comparator {

        public int compare(Object arg0, Object arg1) {
            String s0 = (String) arg0;
            String s1 = (String) arg1;
            int comp = new Integer(s1.length()).compareTo(new Integer(s0
                    .length()));
            if (comp == 0) {
                comp = s0.compareTo(s1);
            }
            return comp;
        }

    }

    public void addMountPoint(String point) {
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        for (Iterator iter = mailboxManagerFactories.iterator(); iter.hasNext();) {
            ContainerUtil.service(iter.next(),serviceManager);
        }

    }

    public synchronized Map getOpenMailboxSessionCountMap() {
        // TEST write a unit test
        Map countMap=new HashMap();
        for (Iterator iter = mailboxManagerFactories.iterator(); iter.hasNext();) {
            MailboxManagerFactory factory = (MailboxManagerFactory) iter.next();
            countMap.putAll(factory.getOpenMailboxSessionCountMap());
        }
        return countMap;
    }


}
