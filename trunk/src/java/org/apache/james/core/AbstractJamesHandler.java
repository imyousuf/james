/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.core;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogTarget;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * Common Handler code
 */
public abstract class AbstractJamesHandler extends AbstractLogEnabled implements ConnectionHandler, Poolable {


    /**
     * The thread executing this handler
     */
    protected Thread handlerThread;

    /**
     * The TCP/IP socket over which the POP3 interaction
     * is occurring
     */
    protected Socket socket;

    /**
     * The writer to which outgoing messages are written.
     */
    protected PrintWriter out;
    
    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    protected Watchdog theWatchdog;

    /**
     * The watchdog target that idles out this handler.
     */
    private WatchdogTarget theWatchdogTarget = new JamesWatchdogTarget();


    public abstract void setConfigurationData(Object theData);


    /**
     * Set the Watchdog for use by this handler.
     *
     * @param theWatchdog the watchdog
     */
    void setWatchdog(Watchdog theWatchdog) {
        this.theWatchdog = theWatchdog;
    }

    /**
     * Gets the Watchdog Target that should be used by Watchdogs managing
     * this connection.
     *
     * @return the WatchdogTarget
     */
    WatchdogTarget getWatchdogTarget() {
        return theWatchdogTarget;
    }

    /**
     * Idle out this connection
     */
    void idleClose() {
        if (getLogger() != null) {
            getLogger().error("POP3 Connection has idled out.");
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            // ignored
        } finally {
            socket = null;
        }

        synchronized (this) {
            // Interrupt the thread to recover from internal hangs
            if (handlerThread != null) {
                handlerThread.interrupt();
                handlerThread = null;
            }
        }

    }

    /**
     * This method logs at a "DEBUG" level the response string that
     * was sent to the POP3 client.  The method is provided largely
     * as syntactic sugar to neaten up the code base.  It is declared
     * private and final to encourage compiler inlining.
     *
     * @param responseString the response string sent to the client
     */
    private final void logResponseString(String responseString) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Sent: " + responseString);
        }
    }

    /**
     * Write and flush a response string.  The response is also logged.
     * Should be used for the last line of a multi-line response or
     * for a single line response.
     *
     * @param responseString the response string sent to the client
     */
    protected final void writeLoggedFlushedResponse(String responseString) {
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Write a response string.  The response is also logged.
     * Used for multi-line responses.
     *
     * @param responseString the response string sent to the client
     */
    protected final void writeLoggedResponse(String responseString) {
        out.println(responseString);
        logResponseString(responseString);
    }


    /**
     * A private inner class which serves as an adaptor
     * between the WatchdogTarget interface and this
     * handler class.
     */
    private class JamesWatchdogTarget
        implements WatchdogTarget {

        /**
         * @see org.apache.james.util.watchdog.WatchdogTarget#execute()
         */
        public void execute() {
            AbstractJamesHandler.this.idleClose();
        }

    }


}
