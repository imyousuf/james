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

package org.apache.james.mailrepository;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.SpoolRepository;

/**
 * Implementation of a SpoolRepository on a database.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br>&lt;repository destinationURL="town://path"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/&gt;
 *  <br>            &lt;driver&gt;sun.jdbc.odbc.JdbcOdbcDriver&lt;/conn&gt;
 *  <br>            &lt;conn&gt;jdbc:odbc:LocalDB&lt;/conn&gt;
 *  <br>            &lt;table&gt;Message&lt;/table&gt;
 *  <br>&lt;/repository&gt;
 * <p>destinationURL specifies..(Serge??)
 * <br>Type can be SPOOL or MAIL
 * <br>Model is currently not used and may be dropped
 * <br>conn is the location of the ...(Serge)
 * <br>table is the name of the table in the Database to be used
 *
 * <p>Requires a logger called MailRepository.
 *
 * <p>Approach for spool manager:
 *
 * PendingMessage inner class
 *
 * accept() is called....
 * checks whether needs to load PendingMessages()
 * tries to get a message()
 * if none, wait 60
 *
 * accept(long) is called
 * checks whether needs to load PendingMessages
 * tries to get a message(long)
 * if none, wait accordingly
 *
 * sync checkswhetherneedstoloadPendingMessages()
 * if pending messages has messages in immediate process, return immediately
 * if run query in last WAIT_LIMIT time, return immediately
 * query and build 2 vectors of Pending messages.
 *  Ones that need immediate processing
 *  Ones that are delayed.  put them in time order
 * return
 *
 * get_a_message()
 * loop through immediate messages.
 *  - remove top message
 *  - try to lock.  if successful, return.  otherwise loop.
 * if nothing, return null
 *
 * get_a_message(long)
 * try get_a_message()
 * check top message in pending.  if ready, then remove, try to lock, return if lock.
 * return null.
 *
 *
 * @version 1.0.0, 24/04/1999
 */
public class JDBCSpoolRepository extends JDBCMailRepository implements SpoolRepository {

    /**
     * How long a thread should sleep when there are no messages to process.
     */
    private static int WAIT_LIMIT = 60000;
    /**
     * How long we have to wait before reloading the list of pending messages
     */
    private static int LOAD_TIME_MININUM = 1000;
    /**
     * A queue in memory of messages that need processing
     */
    private LinkedList pendingMessages = new LinkedList();
    /**
     * When the queue was last read
     */
    private long pendingMessagesLoadTime = 0;
    /**
     * Maximum size of the pendingMessages queue
     */
    private int maxPendingMessages = 0;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        super.configure(conf);
        maxPendingMessages = conf.getChild("maxcache").getValueAsInteger(1000);
    }

    /**
     * Return the key of a message to process.  This is a message in the spool that is not locked.
     */
    public String accept() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            //Loop through until we are either out of pending messages or have a message
            // that we can lock
            PendingMessage next = null;
            while ((next = getNextPendingMessage()) != null && !Thread.currentThread().isInterrupted()) {
                if (lock(next.key)) {
                    return next.key;
                }
            }
            //Nothing to do... sleep!
            try {
                synchronized (this) {
                    //StringBuffer errorBuffer =
                    //    new StringBuffer(128)
                    //            .append("waiting : ")
                    //            .append(WAIT_LIMIT/1000L)
                    //            .append(" in ")
                    //            .append(repositoryName);
                    //System.err.println(errorBuffer.toString());
                    wait(WAIT_LIMIT);
                }
            } catch (InterruptedException ex) {
                throw ex;
            }
        }
        throw new InterruptedException();
    }

    /**
     * Return the key of a message that's ready to process.  If a message is of type "error"
     * then check the last updated time, and don't try it until the long 'delay' parameter
     * milliseconds has passed.
     */
    public synchronized String accept(long delay) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            //Loop through until we are either out of pending messages or have a message
            // that we can lock
            PendingMessage next = null;
            long sleepUntil = 0;
            while ((next = getNextPendingMessage()) != null && !Thread.currentThread().isInterrupted()) {
                //Check whether this is time to expire
                boolean shouldProcess = false;
                if (Mail.ERROR.equals(next.state)) {
                    //if it's an error message, test the time
                    long processingTime = delay + next.lastUpdated;
                    if (processingTime < System.currentTimeMillis()) {
                        //It's time to process
                        shouldProcess = true;
                    } else {
                        //We don't process this, but we want to possibly reduce the amount of time
                        //  we sleep so we wake when this message is ready.
                        if (sleepUntil == 0 || processingTime < sleepUntil) {
                            sleepUntil = processingTime;
                        }
                    }
                } else {
                    shouldProcess = true;
                }
                if (shouldProcess && lock(next.key)) {
                    return next.key;
                }
            }
            //Nothing to do... sleep!
            if (sleepUntil == 0) {
                sleepUntil = System.currentTimeMillis() + WAIT_LIMIT;
            }
            try {
                synchronized (this) {
                    long waitTime = sleepUntil - System.currentTimeMillis();
                    //StringBuffer errorBuffer =
                    //    new StringBuffer(128)
                    //            .append("waiting ")
                    //            .append((waitTime) / 1000L)
                    //            .append(" in ")
                    //            .append(repositoryName);
                    //System.err.println(errorBuffer.toString());
                    wait(waitTime);
                }
            } catch (InterruptedException ex) {
                throw ex;
            }
        }
        throw new InterruptedException();
    }

    /**
     * Needs to override this method and reset the time to load to zero.
     * This will force a reload of the pending messages queue once that
     * is empty... a message that gets added will sit here until that queue
     * time has passed and the list is then reloaded.
     */
    public void store(MailImpl mc) {
        pendingMessagesLoadTime = 0;
        super.store(mc);
    }

    /**
     * If not empty, gets the next pending message.  Otherwise checks
     * checks the last time pending messages was loaded and load if
     * it's been more than 1 second (should be configurable).
     */
    private PendingMessage getNextPendingMessage() {
        //System.err.println("Trying to get next message in " + repositoryName);
        synchronized (pendingMessages) {
            if (pendingMessages.size() == 0 && pendingMessagesLoadTime < System.currentTimeMillis()) {
                pendingMessagesLoadTime = LOAD_TIME_MININUM + System.currentTimeMillis();
                loadPendingMessages();
            }

            if (pendingMessages.size() == 0) {
                return null;
            } else {
                //System.err.println("Returning a pending message in " + repositoryName);
                return (PendingMessage)pendingMessages.removeFirst();
            }
        }
    }

    /**
     * Retrieves the pending messages that are in the database
     */
    private void loadPendingMessages() {
        //Loads a vector with PendingMessage objects
        //System.err.println("loading pending messages in " + repositoryName);
        synchronized (pendingMessages) {
            pendingMessages.clear();

            Connection conn = null;
            PreparedStatement listMessages = null;
            ResultSet rsListMessages = null;
            try {
                conn = datasource.getConnection();
                listMessages =
                    conn.prepareStatement(sqlQueries.getSqlString("listMessagesSQL", true));
                listMessages.setString(1, repositoryName);
                listMessages.setMaxRows(maxPendingMessages);
                rsListMessages = listMessages.executeQuery();
                // Continue to have it loop through the list of messages until we hit
                // a possible message, or we retrieve maxPendingMessages messages.
                // This maxPendingMessages cap is to avoid loading thousands or
                // hundreds of thousands of messages when the spool is enourmous.
                while (rsListMessages.next() && pendingMessages.size() < maxPendingMessages && !Thread.currentThread().isInterrupted()) {
                    String key = rsListMessages.getString(1);
                    String state = rsListMessages.getString(2);
                    long lastUpdated = rsListMessages.getTimestamp(3).getTime();
                    pendingMessages.add(new PendingMessage(key, state, lastUpdated));
                }
            } catch (SQLException sqle) {
                //Log it and avoid reloading for a bit
                getLogger().error("Error retrieving pending messages", sqle);
                pendingMessagesLoadTime = LOAD_TIME_MININUM * 10 + System.currentTimeMillis();
            } finally {
                theJDBCUtil.closeJDBCResultSet(rsListMessages);
                theJDBCUtil.closeJDBCStatement(listMessages);
                theJDBCUtil.closeJDBCConnection(conn);
            }
        }
    }

    /**
     * Simple class to hold basic information about a message in the spool
     */
    class PendingMessage {
        protected String key;
        protected String state;
        protected long lastUpdated;

        public PendingMessage(String key, String state, long lastUpdated) {
            this.key = key;
            this.state = state;
            this.lastUpdated = lastUpdated;
        }
    }
}
