/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util.watchdog;

import org.apache.avalon.cornerstone.services.scheduler.Target;

/**
 * This interface represents an action to be triggered by a watchdog process.
 *
 * @author Andrei Ivanov
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface WatchdogTarget {

    void execute();
}
