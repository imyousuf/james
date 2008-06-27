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

package org.apache.james.util.watchdog;

/**
 * This interface represents an abstract watchdog process that serves to
 * monitor a situation and triggers an action under an implementation-specific
 * trigger condition.
 *
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
