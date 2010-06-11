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

import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

/**
 *  A class to instantiate and schedule a set of mail fetching tasks
 *
 * $Id$
 *
 *  @see org.apache.james.fetchmail.FetchMailOriginal#configure(Configuration) FetchMailOriginal
 *  
 */
public class FetchScheduler
    extends AbstractLogEnabled
    implements Serviceable, Configurable, Initializable, Disposable, FetchSchedulerMBean {

    /**
     * Configuration object for this service
     */
    private Configuration conf;

    /**
     * The service manager that allows access to the system services
     */
    private ServiceManager m_manager;

    /**
     * The scheduler service that is used to trigger fetch tasks.
     */
    private TimeScheduler scheduler;

    /**
     * Whether this service is enabled.
     */
    private volatile boolean enabled = false;

    private ArrayList theFetchTaskNames = new ArrayList();

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service( ServiceManager )
     */
    public void service(ServiceManager comp) throws ServiceException
    {
        m_manager = comp;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException
    {
        this.conf = conf;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        enabled = conf.getAttributeAsBoolean("enabled", false);
        if (enabled)
        {
            scheduler = (TimeScheduler) m_manager.lookup(TimeScheduler.ROLE);
            Configuration[] fetchConfs = conf.getChildren("fetch");
            for (int i = 0; i < fetchConfs.length; i++)
            {
                FetchMail fetcher = new FetchMail();
                Configuration fetchConf = fetchConfs[i];
                String fetchTaskName = fetchConf.getAttribute("name");
                ContainerUtil.enableLogging(fetcher,getLogger().getChildLogger(fetchTaskName));
                ContainerUtil.service(fetcher,m_manager);
                ContainerUtil.configure(fetcher,fetchConf);
                Integer interval =
                    new Integer(fetchConf.getChild("interval").getValue());
                PeriodicTimeTrigger fetchTrigger =
                    new PeriodicTimeTrigger(0, interval.intValue());

                scheduler.addTrigger(fetchTaskName, fetchTrigger, fetcher);
                theFetchTaskNames.add(fetchTaskName);
            }

            if (getLogger().isInfoEnabled())
                getLogger().info("FetchMail Started");
            System.out.println("FetchMail Started");
        }
        else
        {
            if (getLogger().isInfoEnabled())
                getLogger().info("FetchMail Disabled");
            System.out.println("FetchMail Disabled");
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose()
    {
        if (enabled)
        {
            getLogger().info("FetchMail dispose...");
            Iterator nameIterator = theFetchTaskNames.iterator();
            while (nameIterator.hasNext())
            {
                scheduler.removeTrigger((String) nameIterator.next());
            }
            getLogger().info("FetchMail ...dispose end");
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
