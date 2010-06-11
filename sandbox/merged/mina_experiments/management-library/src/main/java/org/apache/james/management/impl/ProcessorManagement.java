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
package org.apache.james.management.impl;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.SpoolManager;
import org.apache.james.management.ProcessorManagementMBean;
import org.apache.james.management.ProcessorManagementService;
import org.apache.james.management.mbean.MatcherManagement;
import org.apache.james.management.mbean.ProcessorDetail;
import org.apache.james.management.mbean.MailetManagement;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MatcherConfig;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * manage processors, mailets and matchers
 */
public class ProcessorManagement implements Serviceable, ProcessorManagementService, ProcessorManagementMBean {

    private SpoolManager processorManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        SpoolManager processorManager = (SpoolManager)serviceManager.lookup(SpoolManager.ROLE);
        setProcessorManager(processorManager);
    }

    @PostConstruct
    public void init() {
        registerMBeans();
    }
    
    private void registerMBeans() {
        ArrayList mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (mBeanServers == null || mBeanServers.size() == 0) return; // no server to publish MBeans

        // take the first one
        MBeanServer mBeanServer = (MBeanServer) mBeanServers.get(0);

        String baseObjectName = "Phoenix:application=james,topic=ProcessorAdministration,block=ProcessorManagement,";

        String[] processorNames = getProcessorNames();
        for (int i = 0; i < processorNames.length; i++) {
            String processorName = processorNames[i];
            createProcessorMBean(baseObjectName, processorName, mBeanServer);
            continue;
        }
    }

    private void createProcessorMBean(String baseObjectName, String processorName, MBeanServer mBeanServer) {
        String processorMBeanName = baseObjectName + "processor=" + processorName;
        ProcessorDetail processorMBean = new ProcessorDetail(processorManager, processorName);
        registerMBean(mBeanServer, processorMBeanName, processorMBean);

        // add all mailets but the last, because that is a terminator (see LinearProcessor.closeProcessorLists())
        List mailetConfigs = processorManager.getMailetConfigs(processorName);
        for (int i = 0; i < mailetConfigs.size()-1; i++) {
            MailetConfig mailetConfig = (MailetConfig) mailetConfigs.get(i);

            String mailetMBeanName = processorMBeanName + ",subtype=mailet,index=" + (i+1) + ",mailetname=" + mailetConfig.getMailetName();
            MailetManagement mailetMBean = new MailetManagement(mailetConfig);
            registerMBean(mBeanServer, mailetMBeanName, mailetMBean);
        }

        // add all matchers but the last, because that is a terminator (see LinearProcessor.closeProcessorLists())
        List matcherConfigs = processorManager.getMatcherConfigs(processorName);
        for (int i = 0; i < matcherConfigs.size()-1; i++) {
            MatcherConfig matcherConfig = (MatcherConfig) matcherConfigs.get(i);

            String matcherMBeanName = processorMBeanName + ",subtype=matcher,index=" + (i+1) + ",matchername=" + matcherConfig.getMatcherName();
            MatcherManagement matcherMBean = new MatcherManagement(matcherConfig);
            registerMBean(mBeanServer, matcherMBeanName, matcherMBean);
        }

    }

    private void registerMBean(MBeanServer mBeanServer, String mBeanName, Object object) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(mBeanName);
        } catch (MalformedObjectNameException e) {
            return; // TODO handle error, log something
        }
        try {
            mBeanServer.registerMBean(object, objectName);
        } catch (javax.management.JMException e) {
            e.printStackTrace(); // TODO change to logger
        }
    }

    public void setProcessorManager(SpoolManager processorManager) {
        this.processorManager = processorManager;
    }

    public String[] getProcessorNames() {
        return processorManager.getProcessorNames();
    }

    public String[] getMailetNames(String processorName) {
        List mailetConfigs = processorManager.getMailetConfigs(processorName);
        // always ommit the terminating mailet
        String[] mailetNames = new String[mailetConfigs.size()-1];
        int i = 0;
        Iterator iterator = mailetConfigs.iterator();
        while (iterator.hasNext()) {
            MailetConfig mailetConfig = (MailetConfig) iterator.next();
            if (!iterator.hasNext()) continue; // ommit the terminating mailet
            String mailetName = mailetConfig.getMailetName();
            mailetNames[i] = mailetName;
            i++;
        }
        return mailetNames;
    }

    public String[] getMatcherNames(String processorName) {
        List matcherConfigs = processorManager.getMatcherConfigs(processorName);
        // always ommit the terminating mailet
        String[] matcherNames = new String[matcherConfigs.size()-1];
        int i = 0;
        Iterator iterator = matcherConfigs.iterator();
        while (iterator.hasNext()) {
            MatcherConfig matcherConfig = (MatcherConfig) iterator.next();
            if (!iterator.hasNext()) continue; // ommit the terminating mailet
            String matcherName = matcherConfig.getMatcherName();
            matcherNames[i] = matcherName;
            i++;
        }
        return matcherNames;
    }
    
    public String[] getMatcherParameters(String processorName, int matcherIndex) {
        List matcherConfigs = processorManager.getMatcherConfigs(processorName);
        if (matcherConfigs == null || matcherConfigs.size() < matcherIndex) return null;
        MatcherConfig matcherConfig = (MatcherConfig)matcherConfigs.get(matcherIndex);
        return new String[] {matcherConfig.getCondition()};
    }

    public String[] getMailetParameters(String processorName, int mailetIndex) {
        List mailetConfigs = processorManager.getMailetConfigs(processorName);
        if (mailetConfigs == null || mailetConfigs.size() < mailetIndex) return null;
        MailetConfig mailetConfig = (MailetConfig) mailetConfigs.get(mailetIndex);
        return MailetManagement.getMailetParameters(mailetConfig);
    }

}
