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

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util.watchdog;

import org.apache.excalibur.thread.ThreadPool ;
import org.apache.avalon.framework.activity.Disposable;
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
        if (watchdogThread != null) 
        {
            if( getLogger().isDebugEnabled() )
            {
                getLogger().debug("Calling reset() " + watchdogThread.getName());
            }
        } else {
            if( getLogger().isDebugEnabled() )
            {
                getLogger().debug("Calling reset() for inactive watchdog");
            }
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
                        if (timeToSleep < 0) {
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
            if (triggerTarget instanceof Disposable) {
                ((Disposable)triggerTarget).dispose();
            }
            triggerTarget = null;
        }
    }
}
