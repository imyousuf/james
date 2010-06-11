/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util.watchdog;

/**
 * This interface represents an abstract watchdog process that serves to
 * monitor a situation and triggers an action under an implementation-specific
 * trigger condition.
 *
 * @author Andrei Ivanov
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface Watchdog {

    /**
     * Start this Watchdog, causing it to begin monitoring.  The Watchdog can
     * be stopped and restarted.
     */
    void start();

    /**
     * Reset this Watchdog.  Resets any conditions in the implementations
     * (time to expiration, etc.) to their original values
     */
    void reset();

    /**
     * Stop this Watchdog, terminating the monitoring condition.  The monitor
     * can be restarted with a call to startWatchdog.
     */
    void stop();
}
