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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.internet.MimeMessage;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.SpoolRepository;
import org.apache.james.util.Lock;
import org.apache.james.util.LockException;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Implementation of a MailRepository on a database.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br><repository destinationURL="db://<datasource>/<repository_name>"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/>
 *  <br></repository>
 * <p>destinationURL specifies..(Serge??)
 * <br>Type can be SPOOL or MAIL
 * <br>Model is currently not used and may be dropped
 *
 * <p>Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class JDBCMailRepository
    extends AbstractLoggable
    implements MailRepository, Component, Configurable, Composable {

    //private SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy MMM dd h:mm:ss a");

    protected Lock lock;
    protected String destination;
    protected String tableName;
    protected String repositoryName;

    private StreamRepository sr = null;

    //The table where this is stored
    private String driverClassName;
    protected String jdbcURL;
    protected String jdbcUsername;   //optional
    protected String jdbcPassword;    //optional

    protected Properties sqlQueries = null;

    public void configure(Configuration conf) throws ConfigurationException {
        destination = conf.getAttribute("destinationURL");
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        try {
            Properties props = new Properties();
            InputStream in = new FileInputStream(destination.substring(5));
            props.load(in);
            in.close();

            driverClassName = props.getProperty("driver");
            jdbcURL = props.getProperty("URL");
            jdbcUsername = props.getProperty("username");   //optional
            jdbcPassword = props.getProperty("password");    //optional

            Class.forName(driverClassName);

            tableName = props.getProperty("table");
            repositoryName = props.getProperty("repository");

            //Loop through and replace <table> with the actual table name in each case
            sqlQueries = new Properties();
            for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                if (!(key.endsWith("SQL"))) {
                    continue;
                }
                String query = props.getProperty(key);
                int i = query.indexOf("<table>");
                if (i > -1) {
                    query = query.substring(0, i) + tableName + query.substring(i + 7);
                }
                //System.err.println(query);
                sqlQueries.put(key, query);
            }


            String filestore = props.getProperty("filestore");

            if (filestore != null) {

                Store store = (Store)componentManager.
                        lookup("org.apache.avalon.cornerstone.services.store.Store");

                //prepare Configurations for stream repositories
                DefaultConfiguration streamConfiguration
                    = new DefaultConfiguration( "repository",
                                                "generated:JDBCMailRepository.compose()" );

                streamConfiguration.setAttribute( "destinationURL", filestore );
                streamConfiguration.setAttribute( "type", "STREAM" );
                streamConfiguration.setAttribute( "model", "SYNCHRONOUS" );
                sr = (StreamRepository) store.select(streamConfiguration);
            }

            lock = new Lock();
            getLogger().debug(this.getClass().getName() + " created according to " + destination);
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error(message, e);
            e.printStackTrace();
            throw new ComponentException(message, e);
        }
    }

    public synchronized void unlock(Object key) {
        if (lock.unlock(key)) {
            notifyAll();
        } else {
            throw new LockException("Your thread does not own the lock of record " + key);
        }
    }

    public synchronized void lock(Object key) {
        if (lock.lock(key)) {
            notifyAll();
        } else {
            throw new LockException("Record " + key + " already locked by another thread");
        }
    }

    public void store(MailImpl mc) {
        //System.err.println("storing " + mc.getName());
        try {
            Connection conn = getConnection();

            //Need to determine whether need to insert this record, or update it.

            //Begin a transaction
            conn.setAutoCommit(false);

            PreparedStatement checkMessageExists = conn.prepareStatement(sqlQueries.getProperty("checkMessageExistsSQL"));
            checkMessageExists.setString(1, mc.getName());
            checkMessageExists.setString(2, repositoryName);
            ResultSet rsExists = checkMessageExists.executeQuery();
            boolean exists = rsExists.next() && rsExists.getInt(1) > 0;
            rsExists.close();
            checkMessageExists.close();

            if (exists) {
                //Update the existing record
                PreparedStatement updateMessage = conn.prepareStatement(sqlQueries.getProperty("updateMessageSQL"));
                updateMessage.setString(1, mc.getState());
                updateMessage.setString(2, mc.getErrorMessage());
                updateMessage.setString(3, mc.getSender().toString());
                StringBuffer recipients = new StringBuffer();
                for (Iterator i = mc.getRecipients().iterator(); i.hasNext(); ) {
                    recipients.append(i.next().toString());
                    if (i.hasNext()) {
                        recipients.append("\r\n");
                    }
                }
                updateMessage.setString(4, recipients.toString());
                updateMessage.setString(5, mc.getRemoteHost());
                updateMessage.setString(6, mc.getRemoteAddr());
                updateMessage.setDate(7, new java.sql.Date(mc.getLastUpdated().getTime()));
                updateMessage.setString(8, mc.getName());
                updateMessage.setString(9, repositoryName);
                updateMessage.execute();
                updateMessage.close();

                //Determine whether the message body has changed, and possibly avoid
                //  updating the database.
                MimeMessage messageBody = mc.getMessage();
                boolean saveBody = false;
                if (messageBody instanceof MimeMessageWrapper) {
                    MimeMessageWrapper message = (MimeMessageWrapper)messageBody;
                    saveBody = message.isModified();
                } else {
                    saveBody = true;
                }

                if (saveBody) {
                    updateMessage = conn.prepareStatement(sqlQueries.getProperty("updateMessageBodySQL"));
                    ByteArrayOutputStream headerOut = new ByteArrayOutputStream();
                    OutputStream bodyOut = null;
                    if (sr == null) {
                        //If there is no filestore, use the byte array to store headers
                        //  and the body
                        bodyOut = headerOut;
                    } else {
                        //Store the body in the stream repository
                        bodyOut = sr.put(mc.getName());
                    }

                    //Write the message to the headerOut and bodyOut.  bodyOut goes straight to the file
                    MimeMessageWrapper.writeTo(messageBody, headerOut, bodyOut);
                    bodyOut.close();

                    //Store the headers in the database
                    updateMessage.setBytes(1, headerOut.toByteArray());
                    updateMessage.setString(2, mc.getName());
                    updateMessage.setString(3, repositoryName);
                    updateMessage.execute();
                    updateMessage.close();
                }
            } else {
                //Insert the record into the database
                PreparedStatement insertMessage = conn.prepareStatement(sqlQueries.getProperty("insertMessageSQL"));
                insertMessage.setString(1, mc.getName());
                insertMessage.setString(2, repositoryName);
                insertMessage.setString(3, mc.getState());
                insertMessage.setString(4, mc.getErrorMessage());
                insertMessage.setString(5, mc.getSender().toString());
                StringBuffer recipients = new StringBuffer();
                for (Iterator i = mc.getRecipients().iterator(); i.hasNext(); ) {
                    recipients.append(i.next().toString());
                    if (i.hasNext()) {
                        recipients.append("\r\n");
                    }
                }
                insertMessage.setString(6, recipients.toString());
                insertMessage.setString(7, mc.getRemoteHost());
                insertMessage.setString(8, mc.getRemoteAddr());
                java.sql.Date lastUpdated = new java.sql.Date(mc.getLastUpdated().getTime());
                insertMessage.setDate(9, lastUpdated);
                MimeMessage messageBody = mc.getMessage();

                ByteArrayOutputStream headerOut = new ByteArrayOutputStream();
                OutputStream bodyOut = null;
                if (sr == null) {
                    //If there is no sr, then use the same byte array to hold the headers
                    //  and the body
                    bodyOut = headerOut;
                } else {
                    //Store the body in the file system.
                    bodyOut = sr.put(mc.getName());
                }

                //Write the message to the headerOut and bodyOut.  bodyOut goes straight to the file
                MimeMessageWrapper.writeTo(messageBody, headerOut, bodyOut);
                bodyOut.close();

                //Store the headers in the database
                insertMessage.setBytes(10, headerOut.toByteArray());
                insertMessage.execute();
                insertMessage.close();
            }

            conn.commit();
            conn.setAutoCommit(true);
            conn.close();

            synchronized (this) {
                notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing mail Container: " + e);
        }
    }

    public MailImpl retrieve(String key) {
        //System.err.println("retrieving " + key);
        try {
            Connection conn = getConnection();

            PreparedStatement retrieveMessage = conn.prepareStatement(sqlQueries.getProperty("retrieveMessageSQL"));
            retrieveMessage.setString(1, key);
            retrieveMessage.setString(2, repositoryName);
            ResultSet rsMessage = retrieveMessage.executeQuery();
            if (!rsMessage.next()) {
                throw new RuntimeException("Did not find a record " + key + " in " + repositoryName);
            }
            MailImpl mc = new MailImpl();
            mc.setName(key);
            mc.setState(rsMessage.getString(1));
            mc.setErrorMessage(rsMessage.getString(2));
            mc.setSender(new MailAddress(rsMessage.getString(3)));
            StringTokenizer st = new StringTokenizer(rsMessage.getString(4), "\r\n", false);
            Set recipients = new HashSet();
            while (st.hasMoreTokens()) {
                recipients.add(new MailAddress(st.nextToken()));
            }
            mc.setRecipients(recipients);
            mc.setRemoteHost(rsMessage.getString(5));
            mc.setRemoteAddr(rsMessage.getString(6));
            mc.setLastUpdated(new java.util.Date(rsMessage.getDate(7).getTime()));

            MimeMessageJDBCSource source = new MimeMessageJDBCSource(this, key, sr);
            MimeMessageWrapper message = new MimeMessageWrapper(source);
            mc.setMessage(message);
            rsMessage.close();
            retrieveMessage.close();
            conn.close();
            return mc;
        } catch (SQLException sqle) {
            System.err.println("Error retrieving message");
            System.err.println(sqle.getMessage());
            System.err.println(sqle.getErrorCode());
            System.err.println(sqle.getSQLState());
            System.err.println(sqle.getNextException());
            sqle.printStackTrace();
            throw new RuntimeException("Exception while retrieving mail: " + sqle.getMessage());
        } catch (Exception me) {
            me.printStackTrace();
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
    }

    public void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public void remove(String key) {
        //System.err.println("removing " + key);
        try {
            lock(key);

            Connection conn = getConnection();
            PreparedStatement removeMessage = conn.prepareStatement(sqlQueries.getProperty("removeMessageSQL"));
            removeMessage.setString(1, key);
            removeMessage.setString(2, repositoryName);
            removeMessage.execute();
            removeMessage.close();
            conn.close();

            if (sr != null) {
                sr.remove(key);
            }
        } catch (Exception me) {
            throw new RuntimeException("Exception while removing mail: " + me.getMessage());
        } finally {
            unlock(key);
        }
    }

    public Iterator list() {
        //System.err.println("listing messages");
        try {
            Connection conn = getConnection();
            PreparedStatement listMessages = conn.prepareStatement(sqlQueries.getProperty("listMessagesSQL"));
            listMessages.setString(1, repositoryName);
            ResultSet rsListMessages = listMessages.executeQuery();

            List messageList = new ArrayList();
            while (rsListMessages.next()) {
                messageList.add(rsListMessages.getString(1));
            }
            rsListMessages.close();
            listMessages.close();
            conn.close();
            return messageList.iterator();
        } catch (Exception me) {
           me.printStackTrace();
            throw new RuntimeException("Exception while listing mail: " + me.getMessage());
        }
    }

    /**
     * Opens a database connection.
     */
    protected Connection getConnection() {
        try {
            if (jdbcUsername == null ) {
                return DriverManager.getConnection(jdbcURL);
            } else {
                return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
            }
        } catch (SQLException sqlExc) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error connecting to database");
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof JDBCMailRepository)) {
            return false;
        }
        JDBCMailRepository repository = (JDBCMailRepository)obj;
        return repository.tableName.equals(tableName) && repository.repositoryName.equals(repositoryName);
    }
}
