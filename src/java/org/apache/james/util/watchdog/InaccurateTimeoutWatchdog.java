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

import org.apache.excalibur.thread.ThreadPool;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * This class represents an watchdog process that serves to
 * monitor a situation and triggers an action after a certain time has
 * passed.  This implementation is deliberately inaccurate, trading
 * accuracy for minimal impact on reset.  This should be used when
 * the time of the Watchdog trigger is not critical, and a high number
 * of resets are expected.
 *
 */
public class InaccurateTimeoutWatchdog
    extends AbstractLogEnabled
    implements Watchdog, Runnable, Disposable {

    /**
     * Whether the watchdog is currently checking the trigger condition
     */
    private volatile boolean isChecking = false;

    /**
     * Whether the watchdog has been reset since the thread slept.
     */
    private volatile boolean isReset = false;


    /**
     * The number of milliseconds until the watchdog times out.
     */
    private final long timeout;

    /**
     * The last time the internal timer was reset, as measured in milliseconds since
     * January 1, 1970 00:00:00.000 GMT.
     */
    private volatile long lastReset;

    /**
     * The WatchdogTarget whose execute() method will be called upon triggering
     * of the condition.
     */
    private WatchdogTarget triggerTarget;

    /**
     * The thread that runs the watchdog.
     */
    private Thread watchdogThread;

    /**
     * The thread pool used to generate InaccurateTimeoutWatchdogs
     */
    private ThreadPool myThreadPool;

    /**
     * The sole constructor for the InaccurateTimeoutWatchdog
     *
     * @param timeout the time (in msec) that it will take the Watchdog to timeout
     * @param target the WatchdogTarget to be executed when this Watchdog expires
     * @param threadPool the thread pool used to generate threads for this implementation.
     */
    public InaccurateTimeoutWatchdog(long timeout, WatchdogTarget target, ThreadPool threadPool) {
        if (target == null) {
            throw new IllegalArgumentException("The WatchdogTarget for this TimeoutWatchdog cannot be null.");
        }
        if (threadPool == null) {
            throw new IllegalArgumentException("The thread pool for this TimeoutWatchdog cannot be null.");
        }
        this.timeout = timeout;
        triggerTarget = target;
        myThreadPool = threadPool;
    }

    /**
     * Start this Watchdog, causing it to begin checking.
     */
    public void start() {
        getLogger().debug("Calling start()");
        lastReset = System.currentTimeMillis();
        isChecking = true;
        synchronized(this) {
            if ( watchdogThread == null) {
                myThreadPool.execute(this);
            }
        }
    }

    /**
     * Reset this Watchdog.  Tells the Watchdog thread to reset
     * the timer when it next awakens.
     */
    public void reset() {
        if (watchdogThread != null) {
            getLogger().debug("Calling reset() " + watchdogThread.getName());
        } else {
            getLogger().debug("Calling reset() for inactive watchdog");
        }
        isReset = true;
    }

    /**
     * Stop this Watchdog, causing the Watchdog to stop checking the trigger
     * condition.  The monitor can be restarted with a call to startWatchdog.
     */
    public void stop() {
        if (watchdogThread != null) {
            getLogger().debug("Calling stop() " + watchdogThread.getName());
        } else {
            getLogger().debug("Calling stop() for inactive watchdog");
        }
        isChecking = false;
    }

    /**
     * Execute the body of the Watchdog, triggering as appropriate.
     */
    public void run() {

        try {
            watchdogThread = Thread.currentThread();

            while ((!(Thread.currentThread().interrupted())) && (watchdogThread != null)) {
                try {
                    if (!isChecking) {
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("Watchdog " + Thread.currentThread().getName() + " is not active - going to exit.");
                        }
                        synchronized (this) {
                            if (!isChecking) {
                                watchdogThread = null;
                            }
                            continue;
                        }
                    } else {
                        long currentTime = System.currentTimeMillis();
                        if (isReset) {
                            isReset = false;
                            lastReset = currentTime;
                        }
                        long timeToSleep = lastReset + timeout - currentTime;
                        if (watchdogThread != null) {
                            getLogger().debug("Watchdog " + watchdogThread.getName() + " has time to sleep " + timeToSleep);
                        } else {
                            getLogger().debug("Watchdog has time to sleep " + timeToSleep);
                        }
                        if (timeToSleep <= 0) {
                            try {
                                synchronized (this) {
                                    if ((isChecking) && (triggerTarget != null)) {
                                        triggerTarget.execute();
                                    }
                                    watchdogThread = null;
                                }
                            } catch (Throwable t) {
                                getLogger().error("Encountered error while executing Watchdog target.", t);
                            }
                            isChecking = false;
                            continue;
                        } else {
                            synchronized(this) {
                                wait(timeToSleep);
                            }
                        }
                    }
                } catch (InterruptedException ie) {
                }
            }

            synchronized( this ) {
                watchdogThread = null;
            }
        } finally {
            // Ensure that the thread is in a non-interrupted state when it gets returned
            // to the pool.
            Thread.currentThread().interrupted();
        }
        getLogger().debug("Watchdog " + Thread.currentThread().getName() + " is exiting run().");
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        synchronized(this) {
            isChecking = false;
            if (watchdogThread != null) {
                getLogger().debug("Calling disposeWatchdog() " + watchdogThread.getName());
            } else {
                getLogger().debug("Calling disposeWatchdog() for inactive watchdog");
            }
            if (watchdogThread != null) {
                watchdogThread = null;
                notifyAll();
            }
            ContainerUtil.dispose(triggerTarget);
            triggerTarget = null;
        }
    }
}
