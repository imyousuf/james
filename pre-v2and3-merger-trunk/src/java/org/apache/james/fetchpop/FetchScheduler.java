/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.fetchpop;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

/**
 *  A class to instantiate and schedule a set of POP mail fetching tasks<br>
 *  @see org.apache.james.fetchpop.FetchPOP#configure(Configuration) FetchPOP
 *
 * @version 1.0.0, 18/06/2000
 */
public class FetchScheduler
    extends AbstractLogEnabled
    implements Serviceable, Configurable, Initializable, Disposable, FetchSchedulerMBean {

    /**
     * Configuration object for this service
     */
    private Configuration conf;

    /**
     * The component manager that allows access to the system services
     */
    private ServiceManager compMgr;

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
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        compMgr = comp;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        enabled = conf.getAttributeAsBoolean("enabled", false);
        if (enabled) {
            scheduler = (TimeScheduler) compMgr.lookup(TimeScheduler.ROLE);
            Configuration[] fetchConfs = conf.getChildren("fetch");
            for (int i = 0; i < fetchConfs.length; i++) {
                FetchPOP fp = new FetchPOP();
                Configuration fetchConf = fetchConfs[i];
                String fetchTaskName = fetchConf.getAttribute("name");
                fp.enableLogging(getLogger().getChildLogger(fetchTaskName));
                fp.compose(compMgr);
                fp.configure(fetchConf);
                Integer interval = new Integer(fetchConf.getChild("interval").getValue());
                PeriodicTimeTrigger fetchTrigger = new PeriodicTimeTrigger(0, interval.intValue());
                scheduler.addTrigger(fetchTaskName, fetchTrigger, fp);
                theFetchTaskNames.add(fetchTaskName);
            }
            getLogger().info("Fetch POP Started");
            System.out.println("Fetch POP Started ");
        } else {
            getLogger().info("Fetch POP Disabled");
            System.out.println("Fetch POP Disabled");
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if (enabled) {
            getLogger().info( "Fetch POP dispose..." );
            Iterator nameIterator = theFetchTaskNames.iterator();
            while (nameIterator.hasNext()) {
                scheduler.removeTrigger((String)nameIterator.next());
            }

            getLogger().info( "Fetch POP ...dispose end" );
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
