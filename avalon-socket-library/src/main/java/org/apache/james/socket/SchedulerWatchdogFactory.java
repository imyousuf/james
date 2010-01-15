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


package org.apache.james.socket;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.james.socket.api.Watchdog;

/**
 * This class is a factory to produce Watchdogs, each of which is associated
 * with a single TimeScheduler Target and a TimeScheduler object.
 *
 * This could be used in James by adding a server configuration
 * parameter:
 *
 *     schedulerWatchdogs = conf.getChild("useSchedulerWatchdogs").getValueAsBoolean(false);
 *
 * getting the TimeScheduler component:
 *
 *     scheduler = (TimeScheduler) compMgr.lookup(TimeScheduler.ROLE);
 *
 * and changing AbstractJamesService.getWatchdogFactory to look
 * something like: 
 *
 *     protected WatchdogFactory getWatchdogFactory() {
 *        WatchdogFactory theWatchdogFactory = null;
 *        if (schedulerWatchdogs) {
 *             theWatchdogFactory = new SchedulerWatchdogFactory(scheduler, timeout);
 *           } else {
 *             theWatchdogFactory = new ThreadPerWatchdogFactory(threadPool, timeout);
 *           }
 *        if (theWatchdogFactory instanceof LogEnabled) {
 *             ((LogEnabled)theWatchdogFactory).enableLogging(getLogger());
 *        }
 *        return theWatchdogFactory;
 *     }
 *
 */
public class SchedulerWatchdogFactory implements WatchdogFactory {

    /**
     * The thread pool used to generate InaccurateTimeoutWatchdogs
     */
    private ScheduledExecutorService myTimeScheduler;

    private long timeout = -1;

    /**
     * Creates the factory and sets the TimeScheduler used to implement
     * the watchdogs.
     *
     * @param theTimeScheduler the scheduler that manages Watchdog triggering
     *                         for Watchdogs produced by this factory
     * @param timeout the timeout for Watchdogs produced by this factory
     */
    public SchedulerWatchdogFactory(ScheduledExecutorService theTimeScheduler, long timeout) {
        this.timeout = timeout;
        myTimeScheduler = theTimeScheduler;
    }

    /**
     * @see org.apache.james.socket.WatchdogFactory#getWatchdog(WatchdogTarget)
     */
    public Watchdog getWatchdog(WatchdogTarget theTarget) {
        return new SchedulerWatchdog(theTarget);
    }

    /**
     * An inner class that acts as an adaptor between the Watchdog
     * interface and the TimeScheduler interface.
     */
    private class SchedulerWatchdog implements Watchdog {

        /**
         * The in-scheduler identifier for this trigger.
         */
        private ScheduledFuture<?> future = null;

        /**
         * The WatchdogTarget that is passed in when this
         * SchedulerWatchdog is initialized
         */
        private WatchdogTarget theWatchdogTarget;

        /**
         * Constructor for the SchedulerWatchdog
         *
         * @param theTarget the target triggered by this Watchdog
         */
        SchedulerWatchdog(WatchdogTarget theTarget) {
            theWatchdogTarget = theTarget;
            
            this.theTarget = new Runnable() {

                public void run() {
                    theWatchdogTarget.execute();
                }
            };
        }

        private Runnable theTarget;
        
        /**
         * Start this Watchdog, causing it to begin monitoring.  The Watchdog can
         * be stopped and restarted.
         */
        public void start() {            
            future = SchedulerWatchdogFactory.this.myTimeScheduler.schedule(theTarget, SchedulerWatchdogFactory.this.timeout, TimeUnit.MILLISECONDS);

        }

        /**
         * Reset this Watchdog.  Resets any conditions in the implementations
         * (time to expiration, etc.) to their original values
         */
        public void reset() {
            future.cancel(false);
            future = SchedulerWatchdogFactory.this.myTimeScheduler.schedule(theTarget, SchedulerWatchdogFactory.this.timeout, TimeUnit.MILLISECONDS);
        }

        /**
         * Stop this Watchdog, terminating the monitoring condition.  The monitor
         * can be restarted with a call to startWatchdog.
         */
        public void stop() {
            future.cancel(false);
        }
    }

}
