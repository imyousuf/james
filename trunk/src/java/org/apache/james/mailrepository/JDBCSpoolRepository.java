/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.internet.MimeMessage;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.core.MailImpl;
import org.apache.james.services.SpoolRepository;
import org.apache.james.util.Lock;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Implementation of a SpoolRepository on a database.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br><repository destinationURL="town://path"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/>
 *  <br>            <driver>sun.jdbc.odbc.JdbcOdbcDriver</conn>
 *  <br>            <conn>jdbc:odbc:LocalDB</conn>
 *  <br>            <table>Message</table>
 *  <br></repository>
 * <p>destinationURL specifies..(Serge??)
 * <br>Type can be SPOOL or MAIL
 * <br>Model is currently not used and may be dropped
 * <br>conn is the location of the ...(Serge)
 * <br>table is the name of the table in the Database to be used
 *
 * <p>Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Serge Knystautas <sergek@lokitech.com>
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
     * Return the key of a message to process.  This is a message in the spool that is not locked.
     */
    public String accept() {
        while (true) {
            //Loop through until we are either out of pending messages or have a message
            // that we can lock
            PendingMessage next = null;
            while ((next = getNextPendingMessage()) != null) {
                if (lock(next.key)) {
                    return next.key;
                }
            }
            //Nothing to do... sleep!
            try {
                synchronized (this) {
                    //System.err.println("waiting : " + WAIT_LIMIT / 1000 + " in " + repositoryName);
                    wait(WAIT_LIMIT);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Return the key of a message that's ready to process.  If a message is of type "error"
     * then check the last updated time, and don't try it until the long 'delay' parameter
     * milliseconds has passed.
     */
    public synchronized String accept(long delay) {
        while (true) {
            //Loop through until we are either out of pending messages or have a message
            // that we can lock
            PendingMessage next = null;
            long sleepUntil = 0;
            while ((next = getNextPendingMessage()) != null) {
                //Check whether this is time to expire
                boolean shouldProcess = false;
                if (next.state.equals(Mail.ERROR)) {
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
                    //System.err.println("waiting " + (sleepUntil - System.currentTimeMillis()) / 1000 + " in " + repositoryName);
                    wait(sleepUntil - System.currentTimeMillis());
                }
            } catch (InterruptedException ignored) {
            }

        }
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
                rsListMessages = listMessages.executeQuery();
                //Continue to have it loop through the list of messages until we hit
                //  a possible message, or we retrieve 1000 messages.  This 1000 cap is to
                //  avoid loading thousands or hundreds of thousands of messages when the
                //  spool is enourmous.
                while (rsListMessages.next() && pendingMessages.size() < 1000) {
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
                try {
                    rsListMessages.close();
                } catch (Exception e) {
                }
                try {
                    listMessages.close();
                } catch (Exception e) {
                }
                try {
                    conn.close();
                } catch (Exception e) {
                }
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
