/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.fetchpop;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.MailServer;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *  A class to instantiate and schedule a set of POP mail fetching tasks<br>
 *  @see org.apache.james.fetchpop.FetchPOP#configure(Configuration) FetchPOP
 *
 * @version 1.0.0, 18/06/2000
 */
public class FetchScheduler
    extends AbstractLogEnabled
    implements Serviceable, Configurable, Initializable, Disposable {

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

}
