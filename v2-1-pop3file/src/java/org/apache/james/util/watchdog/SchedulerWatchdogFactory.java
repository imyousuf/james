/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util.watchdog;

import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;

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
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class SchedulerWatchdogFactory implements WatchdogFactory {

    /**
     * The thread pool used to generate InaccurateTimeoutWatchdogs
     */
    private TimeScheduler myTimeScheduler;

    private long timeout = -1;

    /**
     * Creates the factory and sets the TimeScheduler used to implement
     * the watchdogs.
     *
     * @param theTimeScheduler the scheduler that manages Watchdog triggering
     *                         for Watchdogs produced by this factory
     * @param timeout the timeout for Watchdogs produced by this factory
     */
    public SchedulerWatchdogFactory(TimeScheduler theTimeScheduler, long timeout) {
        this.timeout = timeout;
        myTimeScheduler = theTimeScheduler;
    }

    /**
     * @see org.apache.james.util.watchdog.WatchdogFactory#getWatchdog(WatchdogTarget)
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
        private String triggerID = null;

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
            // TODO: This should be made more robust then just
            //       using toString()
            triggerID = this.toString();
            theWatchdogTarget = theTarget;
        }

        /**
         * Start this Watchdog, causing it to begin monitoring.  The Watchdog can
         * be stopped and restarted.
         */
        public void start() {
            PeriodicTimeTrigger theTrigger = new PeriodicTimeTrigger((int)SchedulerWatchdogFactory.this.timeout, -1);
            Target theTarget = new Target() {
                                    public void targetTriggered(String targetID) {
                                        theWatchdogTarget.execute();
                                    }
                               };
            SchedulerWatchdogFactory.this.myTimeScheduler.addTrigger(triggerID, theTrigger, theTarget);
        }

        /**
         * Reset this Watchdog.  Resets any conditions in the implementations
         * (time to expiration, etc.) to their original values
         */
        public void reset() {
            SchedulerWatchdogFactory.this.myTimeScheduler.resetTrigger(triggerID);
        }

        /**
         * Stop this Watchdog, terminating the monitoring condition.  The monitor
         * can be restarted with a call to startWatchdog.
         */
        public void stop() {
            SchedulerWatchdogFactory.this.myTimeScheduler.removeTrigger(triggerID);
        }
    }

}
