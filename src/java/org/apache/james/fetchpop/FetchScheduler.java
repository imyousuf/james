/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.fetchpop;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.activity.Disposable;
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

import java.util.ArrayList;
import java.util.Iterator;

/**
 *  A class to instantiate and schedule a set of POP mail fetching tasks<br>
 * <br>$Id: FetchScheduler.java,v 1.4.4.3 2004/02/18 21:47:27 hilmer Exp $
 *  @author <A href="mailto:danny@apache.org">Danny Angus</a>
 *  @see org.apache.james.fetchpop.FetchPOP#configure(Configuration) FetchPOP
 *  
 */
public class FetchScheduler
    extends AbstractLogEnabled
    implements Component, Composable, Configurable, Initializable, Disposable, FetchSchedulerMBean {

    /**
     * Configuration object for this service
     */
    private Configuration conf;

    /**
     * The component manager that allows access to the system services
     */
    private ComponentManager compMgr;

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
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager comp) throws ComponentException {
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
