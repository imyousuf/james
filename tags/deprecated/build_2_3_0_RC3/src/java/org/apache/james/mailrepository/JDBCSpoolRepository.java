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



package org.apache.james.mailrepository;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

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
     * Return a message to process.  This is a message in the spool that is not locked.
     */
    public synchronized Mail accept() throws InterruptedException {
        return accept(new SpoolRepository.AcceptFilter () {
            public boolean accept (String _, String __, long ___, String ____) {
                return true;
            }

            public long getWaitTime () {
                return 0;
            }
        });
    }

    /**
     * Return a message that's ready to process.  If a message is of type "error"
     * then check the last updated time, and don't try it until the long 'delay' parameter
     * milliseconds has passed.
     */
    public synchronized Mail accept(final long delay) throws InterruptedException {
        return accept (new SpoolRepository.AcceptFilter () {
            long sleepUntil = 0;
                
                public boolean accept (String key, String state, long lastUpdated, String errorMessage) {
                    if (Mail.ERROR.equals(state)) {
                        //if it's an error message, test the time
                        long processingTime = delay + lastUpdated;
                        if (processingTime < System.currentTimeMillis()) {
                            //It's time to process
                            return true;
                        } else {
                            //We don't process this, but we want to possibly reduce the amount of time
                            //  we sleep so we wake when this message is ready.
                            if (sleepUntil == 0 || processingTime < sleepUntil) {
                                sleepUntil = processingTime;
                            }
                            return false;
                        }
                    } else {
                        return true;
                    }
                }
                

                public long getWaitTime () {
                    if (sleepUntil == 0) {
                        // in AvalonSpoolRepository we return 0: why do we change sleepUntil?
                        // sleepUntil = System.currentTimeMillis();
                        return 0;
                    }
                    long waitTime = sleepUntil - System.currentTimeMillis();
                    sleepUntil = 0;
                    return waitTime <= 0 ? 1 : waitTime;
                }
                
            });
    }

    /**
     * Returns an arbitrarily selected mail deposited in this Repository for
     * which the supplied filter's accept method returns true.
     * Usage: RemoteDeliverySpool calls accept(filter) with some a filter which determines
     * based on number of retries if the mail is ready for processing.
     * If no message is ready the method will block until one is, the amount of time to block is
     * determined by calling the filters getWaitTime method.
     *
     * @return  the mail
     */
    public synchronized Mail accept(SpoolRepository.AcceptFilter filter) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            //Loop through until we are either out of pending messages or have a message
            // that we can lock
            PendingMessage next = null;
            while ((next = getNextPendingMessage(filter)) != null && !Thread.currentThread().isInterrupted()) {
                //Check whether this is time to expire
                
                // boolean shouldProcess = filter.accept (next.key, next.state, next.lastUpdated, next.errorMessage);
                
                if (/*shouldProcess && */ lock(next.key)) {
                    try {
                        Mail mail = retrieve(next.key);
                        // Retrieve can return null if the mail is no longer on the spool
                        // (i.e. another thread has gotten to it first).
                        // In this case we simply continue to the next key
                        if (mail == null) {
                            unlock(next.key);
                            continue;
                        }
                        return mail;
                    } catch (javax.mail.MessagingException e) {
                        unlock(next.key);
                        getLogger().error("Exception during retrieve -- skipping item " + next.key, e);
                    }
                }
            }
            //Nothing to do... sleep!
            long wait_time = filter.getWaitTime();
            if (wait_time <= 0) {
                wait_time = WAIT_LIMIT;
            }
            try {
                wait (wait_time);
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
    public void store(Mail mc) throws javax.mail.MessagingException {
        pendingMessagesLoadTime = 0;
        super.store(mc);
    }

    /**
     * If not empty, gets the next pending message.  Otherwise checks
     * checks the last time pending messages was loaded and load if
     * it's been more than 1 second (should be configurable).
     */
    private PendingMessage getNextPendingMessage(SpoolRepository.AcceptFilter filter) {
        synchronized (pendingMessages) {
            if (pendingMessages.size() == 0 && pendingMessagesLoadTime < System.currentTimeMillis()) {
                // pendingMessagesLoadTime = LOAD_TIME_MININUM + System.currentTimeMillis();
                loadPendingMessages(filter);
                pendingMessagesLoadTime = Math.max(filter.getWaitTime(), LOAD_TIME_MININUM) + System.currentTimeMillis();
            }

            if (pendingMessages.size() == 0) {
                return null;
            } else {
                return (PendingMessage)pendingMessages.removeFirst();
            }
        }
    }

    /**
     * Retrieves the pending messages that are in the database
     */
    private void loadPendingMessages(SpoolRepository.AcceptFilter filter) {
        //Loads a vector with PendingMessage objects
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
                // Too simplistic.  When filtering, we may need to see
                // more than just maxPendingMessages to load the
                // cache, so just hope that the driver and server use
                // cursors properly.
                // --> listMessages.setMaxRows(maxPendingMessages);
                rsListMessages = listMessages.executeQuery();
                // Continue to have it loop through the list of messages until we hit
                // a possible message, or we retrieve maxPendingMessages messages.
                // This maxPendingMessages cap is to avoid loading thousands or
                // hundreds of thousands of messages when the spool is enourmous.
                while (rsListMessages.next() && pendingMessages.size() < maxPendingMessages && !Thread.currentThread().isInterrupted()) {
                    String key = rsListMessages.getString(1);
                    String state = rsListMessages.getString(2);
                    long lastUpdated = rsListMessages.getTimestamp(3).getTime();
                    String errorMessage = rsListMessages.getString(4);
                    if (filter.accept(key, state, lastUpdated, errorMessage)) {
                        pendingMessages.add(new PendingMessage(key, state, lastUpdated, errorMessage));
                    }
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
        protected String errorMessage;

        public PendingMessage(String key, String state, long lastUpdated, String errorMessage) {
            this.key = key;
            this.state = state;
            this.lastUpdated = lastUpdated;
            this.errorMessage = errorMessage;
        }
    }
}
