/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util.watchdog;

/**
 * This interface represents a factory for producing Watchdogs.
 *
 * @author Andrei Ivanov
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface WatchdogFactory {

    /**
     * Gets a Watchdog
     *
     * @param theTarget the WatchdogTarget to be triggered upon expiration
     */
    Watchdog getWatchdog(WatchdogTarget theTarget) throws Exception;

}
