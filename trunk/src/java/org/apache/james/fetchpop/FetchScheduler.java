/**
 * FetchScheduler.java
 * 
 * Copyright (C) 24-Sep-2002 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file. 
 *
 * Danny Angus
 */
package org.apache.james.fetchpop;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.MailServer;
/**
 *  A class to instantiate and schedule a set of POP mail fetching tasks<br>
 * <br>$Id: FetchScheduler.java,v 1.1 2002/09/24 15:36:30 danny Exp $
 *  @author <A href="mailto:danny@apache.org">Danny Angus</a>
 *  @see org.apache.james.fetchpop.FetchPOP#configure(Configuration) FetchPOP
 *  
 */
public class FetchScheduler
    extends AbstractLogEnabled
    implements Component, Configurable, Initializable, Composable {
    private Configuration conf;
    private MailServer server;
    private DefaultComponentManager compMgr;
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
        if (conf.getAttribute("enabled").equalsIgnoreCase("true")) {
            TimeScheduler scheduler = (TimeScheduler) compMgr.lookup(TimeScheduler.ROLE);
            Configuration[] fetchConfs = conf.getChildren("fetch");
            for (int i = 0; i < fetchConfs.length; i++) {
                FetchPOP fp = new FetchPOP();
                Configuration fetchConf = fetchConfs[i];
                fp.configure(
                    fetchConf,
                    (MailServer) compMgr.lookup(MailServer.ROLE),
                    getLogger().getChildLogger(fetchConf.getAttribute("name")));
                Integer interval = new Integer(fetchConf.getChild("interval").getValue());
                PeriodicTimeTrigger fetchTrigger = new PeriodicTimeTrigger(0, interval.intValue());
                scheduler.addTrigger(fetchConf.getAttribute("name"), fetchTrigger, fp);
            }
            System.out.println("Fetch POP Started ");
        } else {
            getLogger().info("Fetch POP Disabled");
            System.out.println("Fetch POP Disabled");
        }
    }
    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager comp) throws ComponentException {
        compMgr = new DefaultComponentManager(comp);
    }
}
