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
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.internet.MimeMessage;
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
public class JDBCMailRepository
    extends AbstractLoggable
    implements MailRepository, Component, Configurable, Composable {

    private SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy MMM dd h:mm:ss a");

    protected Lock lock;
    protected String destination;
    protected String tableName = "EML_Spool";
    protected String repositoryName;

    //The table where this is stored
    private String driverClassName = "com.inet.tds.TdsDriver";
    protected String jdbcURL = "jdbc:inetdae7:127.0.0.1?database=James";
    protected String jdbcUsername = "sa";   //optional
    protected String jdbcPassword = "rufus4811";    //optional


    private String checkMessageExistsSQL =
            "SELECT count(*) FROM " + tableName + " WHERE message_name = ? AND repository_name = ?";

    private String updateMessageSQL =
            "UPDATE " + tableName + " SET message_state = ?, error_message = ?, sender = ?, recipients = ?, "
            + "remote_host = ?, remote_addr = ?, last_updated = ? "
            + "WHERE message_name = ? AND repository_name = ?";

    private String updateMessageBodySQL =
            "UPDATE " + tableName + " SET message_body = ? WHERE message_name = ? AND repository_name = ?";

    private String insertMessageSQL =
            "INSERT INTO " + tableName + " (message_name, repository_name, message_state, "
            + "error_message, sender, recipients, remote_host, remote_addr, last_updated, message_body) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private String retrieveMessageSQL =
            "SELECT message_state, error_message, sender, recipients, remote_host, remote_addr, last_updated "
            + "FROM " + tableName + " WHERE message_name = ? AND repository_name = ?";

    private String removeMessageSQL =
            "DELETE FROM " + tableName + " WHERE message_name = ? AND repository_name = ?";

    private String listMessagesSQL =
            "SELECT message_name FROM " + tableName + " WHERE repository_name = ? ORDER BY last_updated ASC";

    public void configure(Configuration conf) throws ConfigurationException {
        destination = conf.getAttribute("destinationURL");
        destination = destination.substring(destination.indexOf("//") + 2);
        tableName = destination.substring(0, destination.indexOf("/"));
        repositoryName = destination.substring(destination.indexOf("/") + 1);
        /*
        String checkType = conf.getAttribute("type");
        if (! (checkType.equals("MAIL") || checkType.equals("SPOOL")) ) {
            final String message =
                "Attempt to configure TownSpoolRepository as " + checkType;
            getLogger().warn( message );
            throw new ConfigurationException( message );
        }
        // ignore model
        conndefinition = conf.getChild("conn").getValue();
        tableName = conf.getChild("table").getValue();
        */
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException cnfe) {
            String message = "Unable to load JDBC driver: " + driverClassName;
            getLogger().error(message);
            throw new ConfigurationException(message);
        }
        //lock = new Lock();
        System.err.println("jdbc spool initialized for " + repositoryName);
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        try {
            //store = (Store)componentManager.
            //    lookup( "org.apache.avalon.cornerstone.services.store.Store" );

            //prepare Configurations for object and stream repositories
            DefaultConfiguration objectConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:JDBCMailRepository.compose()" );

            objectConfiguration.setAttribute("destinationURL", destination);
            objectConfiguration.setAttribute("type", "OBJECT");
            objectConfiguration.setAttribute("model", "SYNCHRONOUS");

            DefaultConfiguration streamConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:JDBCMailRepository.compose()" );

            streamConfiguration.setAttribute( "destinationURL", destination );
            streamConfiguration.setAttribute( "type", "STREAM" );
            streamConfiguration.setAttribute( "model", "SYNCHRONOUS" );

            //sr = (StreamRepository) store.select(streamConfiguration);
            //or = (ObjectRepository) store.select(objectConfiguration);
            lock = new Lock();
        getLogger().debug(this.getClass().getName() + " created in " + destination);
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error( message, e );
            throw new ComponentException( message, e );
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
        System.err.println("storing " + mc.getName());
        try {
            Connection conn = getConnection();

            //Need to determine whether need to insert this record, or update it.

            //Begin a transaction
            conn.setAutoCommit(false);

            PreparedStatement checkMessageExists = conn.prepareStatement(checkMessageExistsSQL);
            checkMessageExists.setString(1, mc.getName());
            checkMessageExists.setString(2, repositoryName);
            ResultSet rsExists = checkMessageExists.executeQuery();
            boolean exists = rsExists.next() && rsExists.getInt(1) > 0;
            rsExists.close();
            checkMessageExists.close();

            if (exists) {
                //Update the existing record
                PreparedStatement updateMessage = conn.prepareStatement(updateMessageSQL);
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
                    updateMessage = conn.prepareStatement(updateMessageBodySQL);
                    int size = (int)messageBody.getSize();
                    updateMessage.setBinaryStream(1, new DebugInputStream(messageBody.getInputStream()), size);
                    updateMessage.setString(2, mc.getName());
                    updateMessage.setString(3, repositoryName);
                    updateMessage.execute();
                    updateMessage.close();
                }
            } else {
                //Insert the record into the database
                PreparedStatement insertMessage = conn.prepareStatement(insertMessageSQL);
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
                //java.sql.Date lastUpdated = new java.sql.Date(mc.getLastUpdated().getTime());
                //System.err.println(lastUpdated);
                //insertMessage.setDate(9, lastUpdated);
                insertMessage.setString(9, sqlFormat.format(mc.getLastUpdated()));
                MimeMessage messageBody = mc.getMessage();
                int size = messageBody.getSize();
                insertMessage.setBinaryStream(10, new DebugInputStream(messageBody.getInputStream()), size);
                insertMessage.execute();
                insertMessage.close();
            }

            conn.commit();
            conn.setAutoCommit(true);
            conn.close();

            synchronized (this) {
                System.err.println("everything is notified on " + this);
                notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing mail Container: " + e);
        }
    }

    public MailImpl retrieve(String key) {
        System.err.println("retrieving " + key);
        try {
            Connection conn = getConnection();

            PreparedStatement retrieveMessage = conn.prepareStatement(retrieveMessageSQL);
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

            //Create a reference to a JDBCMimeMessageInputStream
            //InputStream in = new JDBCMimeMessageInputStream(this, key);
            //InputStream in = new TownMimeMessageInputStream(conndefinition, tableName, key, repositoryName);
            //InputStream in = new ByteArrayInputStream(message.getAsBytes("message_body"));
            MimeMessageJDBCSource source = new MimeMessageJDBCSource(this, key);
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
        System.err.println("removing " + key);
        try {
            lock(key);

            Connection conn = getConnection();
            PreparedStatement removeMessage = conn.prepareStatement(removeMessageSQL);
            removeMessage.setString(1, key);
            removeMessage.setString(2, repositoryName);
            removeMessage.execute();
            removeMessage.close();
            conn.close();
        } catch (Exception me) {
            throw new RuntimeException("Exception while removing mail: " + me.getMessage());
        } finally {
            unlock(key);
        }
    }

    public Iterator list() {
        System.err.println("listing messages");
        try {
            Connection conn = getConnection();
            PreparedStatement listMessages = conn.prepareStatement(listMessagesSQL);
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

    //
    // Private methods
    //
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
