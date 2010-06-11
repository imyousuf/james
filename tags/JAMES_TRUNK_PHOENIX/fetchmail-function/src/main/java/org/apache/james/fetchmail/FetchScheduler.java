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


 
package org.apache.james.fetchmail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.services.MailServer;

/**
 *  A class to instantiate and schedule a set of mail fetching tasks
 *
 * $Id$
 *
 */
public class FetchScheduler implements FetchSchedulerMBean {

    /**
     * Configuration object for this service
     */
    private HierarchicalConfiguration conf;


    /**
     * The scheduler service that is used to trigger fetch tasks.
     */
    private TimeScheduler scheduler;

    /**
     * Whether this service is enabled.
     */
    private volatile boolean enabled = false;

    private ArrayList<String> theFetchTaskNames = new ArrayList<String>();


    private DNSService dns;


    private MailServer mailserver;


    private UsersRepository urepos;


    private Log logger;

    @Resource(name="scheduler")
    public void setTimeScheduler(TimeScheduler scheduler) {
        this.scheduler = scheduler;
    }

    
    @Resource(name="dnsserver")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }


    @Resource(name="James")
    public void setMailServer(MailServer mailserver) {
        this.mailserver = mailserver;
    }
   
    @Resource(name="localusersrepository")
    public void setUsersRepository(UsersRepository urepos) {
        this.urepos = urepos;
    }
    
    @Resource(name="org.apache.commons.logging.Log")
    public final void setLogger(Log logger) {
        this.logger = logger;
    }
    
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public final void setConfiguration(HierarchicalConfiguration config) {
        this.conf = config;
    }
    
    
    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() throws Exception
    {
        enabled = conf.getBoolean("[@enabled]", false);
        if (enabled)
        {

            List<HierarchicalConfiguration> fetchConfs = conf.configurationsAt("fetch");
            for (int i = 0; i < fetchConfs.size(); i++)
            {
                // read configuration
                HierarchicalConfiguration fetchConf = fetchConfs.get(i);
                String fetchTaskName = fetchConf.getString("[@name]");
                Integer interval = new Integer(fetchConf.getInt("interval"));

                FetchMail fetcher = new FetchMail();
                fetcher.setConfiguration(fetchConf);
                fetcher.setDNSService(dns);
                fetcher.setMailServer(mailserver);
                fetcher.setUsersRepository(urepos);
                fetcher.setLogger(logger);
                // avalon specific initialization
               // ContainerUtil.enableLogging(fetcher,getLogger().getChildLogger(fetchTaskName));


                // initialize scheduling
                PeriodicTimeTrigger fetchTrigger =
                        new PeriodicTimeTrigger(0, interval.intValue());
                scheduler.addTrigger(fetchTaskName, fetchTrigger, fetcher);
                theFetchTaskNames.add(fetchTaskName);
            }

            if (logger.isInfoEnabled()) logger.info("FetchMail Started");
            System.out.println("FetchMail Started");
        }
        else
        {
            if (logger.isInfoEnabled()) logger.info("FetchMail Disabled");
            System.out.println("FetchMail Disabled");
        }
    }

    @PreDestroy
    public void dispose()
    {
        if (enabled)
        {
            logger.info("FetchMail dispose...");
            Iterator<String> nameIterator = theFetchTaskNames.iterator();
            while (nameIterator.hasNext())
            {
                scheduler.removeTrigger(nameIterator.next());
            }
            logger.info("FetchMail ...dispose end");
        }
    }
    
    /**
     * Describes whether this service is enabled by configuration.
     *
     * @return is the service enabled.
     */
    public final boolean isEnabled() {
        return enabled;
    }
    
}
