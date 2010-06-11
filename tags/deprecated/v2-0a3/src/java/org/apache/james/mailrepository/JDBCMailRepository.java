/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.datasource.DataSourceSelector;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.phoenix.BlockContext;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.services.MailRepository;
import org.apache.james.util.Lock;
import org.apache.james.util.SqlResources;
import org.apache.mailet.MailAddress;

import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.sql.*;
import java.util.*;

/**
 * Implementation of a MailRepository on a database.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br><repository destinationURL="db://<datasource>/<table_name>/<repository_name>"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/>
 *  <br></repository>
 * <p>destinationURL specifies..(Serge??)
 * <br>Type can be SPOOL or MAIL
 * <br>Model is currently not used and may be dropped
 *
 * <p>Requires a logger called MailRepository.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Darrell DeBoer <dd@bigdaz.com>
 * @version 1.0.0, 24/04/1999
 */
public class JDBCMailRepository
    extends AbstractLogEnabled
    implements MailRepository, Component, Contextualizable, Composable, Configurable, Initializable {

    private static final boolean DEEP_DEBUG = false;


    protected Context context;
    private Lock lock;

    // Configuration elements
    protected String destination;
    protected String tableName;
    protected String repositoryName;
    protected String filestore;
    protected String sqlFileName;

    private StreamRepository sr = null;

    //The data-source for this repository
    protected DataSourceSelector datasources;
    protected DataSourceComponent datasource;
    protected String datasourceName;

    // Contains all of the sql strings for this component.
    protected SqlResources sqlQueries;


    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    public void configure(Configuration conf) throws ConfigurationException {
        getLogger().debug(this.getClass().getName() + ".configure()");

        destination = conf.getAttribute("destinationURL");
        // normalise the destination, to simplify processing.
        if ( ! destination.endsWith("/") ) {
            destination += "/";
        }
        // Parse the DestinationURL for the name of the datasource,
        // the table to use, and the (optional) repository Key.
        // Split on "/", starting after "db://"
        List urlParams = new ArrayList();
        int start = 5;
        if (destination.startsWith("dbfile")) {
            //this is dbfile:// instead of db://
            start += 4;
        }
        int end = destination.indexOf('/', start);
        while ( end > -1 ) {
            urlParams.add(destination.substring(start, end));
            start = end + 1;
            end = destination.indexOf('/', start);
        }

        // Build SqlParameters and get datasource name from URL parameters
        if (urlParams.size() == 0) {
            throw new ConfigurationException
                ("Malformed destinationURL - Must be of the format '" +
                 "db://<data-source>[/<table>[/<repositoryName>]]'.  Was passed " + conf.getAttribute("destinationURL"));
        }
        if (urlParams.size() >= 1) {
            datasourceName = (String)urlParams.get(0);
        }
        if (urlParams.size() >= 2) {
            tableName = (String)urlParams.get(1);
        }
        if (urlParams.size() >= 3) {
            repositoryName = "";
            for (int i = 2; i < urlParams.size(); i++) {
                if (i >= 3) {
                    repositoryName += '/';
                }
                repositoryName += (String)urlParams.get(i);
            }
        }

        getLogger().debug("Parsed URL: table = '" + tableName +
                          "', repositoryName = '" + repositoryName + "'");

        filestore = conf.getChild("filestore").getValue(null);
        sqlFileName = conf.getChild("sqlFile").getValue();
        if (!sqlFileName.startsWith("file://")) {
            throw new ConfigurationException
                ("Malformed sqlFile - Must be of the format 'file://<filename>'.");
        }
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        getLogger().debug(this.getClass().getName() + ".compose()");

        // Get the DataSourceSelector block
        datasources = (DataSourceSelector)componentManager.lookup( DataSourceSelector.ROLE );

        try {
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

                getLogger().debug("Got filestore for JdbcMailRepository: " + filestore);
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

    /**
     * Initialises the JDBC repository.
     * 1) Tests the connection to the database.
     * 2) Loads SQL strings from the SQL definition file,
     *     choosing the appropriate SQL for this connection,
     *     and performing paramter substitution,
     * 3) Initialises the database with the required tables, if necessary.
     *
     */
    public void initialize() throws Exception {
        getLogger().debug(this.getClass().getName() + ".initialize()");

        // Get the data-source required.
        datasource = (DataSourceComponent)datasources.select(datasourceName);

        // Test the connection to the database, by getting the DatabaseMetaData.
        Connection conn = datasource.getConnection();

        try {
            // Initialise the sql strings.
            String fileName = sqlFileName.substring("file://".length());
            fileName = ((BlockContext)context).getBaseDirectory() +
                        File.separator + fileName;
            File sqlFile = (new File(fileName)).getCanonicalFile();

            String resourceName = "org.apache.james.mailrepository.JDBCMailRepository";

            getLogger().debug("Reading SQL resources from file: " +
                              sqlFile.getAbsolutePath() + ", section " +
                              this.getClass().getName() + ".");

            // Build the statement parameters
            Map sqlParameters = new HashMap();
            if (tableName != null) {
                sqlParameters.put("table", tableName);
            }
            if (repositoryName != null) {
                sqlParameters.put("repository", repositoryName);
            }

            sqlQueries = new SqlResources();
            sqlQueries.init(sqlFile, this.getClass().getName(),
                            conn, sqlParameters);

            // Check if the required table exists. If not, create it.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (! ( tableExists(dbMetaData, tableName) ||
                    tableExists(dbMetaData, tableName.toUpperCase()) ||
                    tableExists(dbMetaData, tableName.toLowerCase()) ))  {
                // Users table doesn't exist - create it.
                PreparedStatement createStatement =
                    conn.prepareStatement(sqlQueries.getSqlString("createTable", true));
                createStatement.execute();
                createStatement.close();

                getLogger().info("JdbcMailRepository: Created table '" +
                                 tableName + "'.");
            }

        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean tableExists(DatabaseMetaData dbMetaData, String tableName)
        throws SQLException {
        ResultSet rsTables = dbMetaData.getTables(null, null, tableName, null);
        boolean found = rsTables.next();
        rsTables.close();
        return found;
    }

    public synchronized boolean unlock(String key) {
        if (lock.unlock(key)) {
            notifyAll();
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean lock(String key) {
        if (lock.lock(key)) {
            //notifyAll();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Store this message to the database.  Optionally stores the message
     * body to the filesystem and only writes the headers to the database.
     */
    public void store(MailImpl mc) {
        //System.err.println("storing " + mc.getName());
        Connection conn = null;
        try {
            conn = datasource.getConnection();

            //Need to determine whether need to insert this record, or update it.

            //Begin a transaction
            conn.setAutoCommit(false);

            PreparedStatement checkMessageExists =
                conn.prepareStatement(sqlQueries.getSqlString("checkMessageExistsSQL", true));
            checkMessageExists.setString(1, mc.getName());
            checkMessageExists.setString(2, repositoryName);
            ResultSet rsExists = checkMessageExists.executeQuery();
            boolean exists = rsExists.next() && rsExists.getInt(1) > 0;
            rsExists.close();
            checkMessageExists.close();

            if (exists) {
                //Update the existing record
                PreparedStatement updateMessage =
                    conn.prepareStatement(sqlQueries.getSqlString("updateMessageSQL", true));
                updateMessage.setString(1, mc.getState());
                updateMessage.setString(2, mc.getErrorMessage());
                if (mc.getSender() == null) {
                    updateMessage.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    updateMessage.setString(3, mc.getSender().toString());
                }
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
                updateMessage.setTimestamp(7, new java.sql.Timestamp(mc.getLastUpdated().getTime()));
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
                    updateMessage =
                        conn.prepareStatement(sqlQueries.getSqlString("updateMessageBodySQL", true));
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
                PreparedStatement insertMessage =
                    conn.prepareStatement(sqlQueries.getSqlString("insertMessageSQL", true));
                insertMessage.setString(1, mc.getName());
                insertMessage.setString(2, repositoryName);
                insertMessage.setString(3, mc.getState());
                insertMessage.setString(4, mc.getErrorMessage());
                if (mc.getSender() == null) {
                    insertMessage.setNull(5, java.sql.Types.VARCHAR);
                } else {
                    insertMessage.setString(5, mc.getSender().toString());
                }
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
                insertMessage.setTimestamp(9, new java.sql.Timestamp(mc.getLastUpdated().getTime()));
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

            synchronized (this) {
                notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing mail Container: " + e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    //ignore
                }
            }
        }
    }

    public MailImpl retrieve(String key) {
        if (DEEP_DEBUG) {
            System.err.println("retrieving " + key);
        }
        Connection conn = null;
        try {
            conn = datasource.getConnection();
            if (DEEP_DEBUG) {
                System.err.println("got a conn " + key);
            }

            PreparedStatement retrieveMessage =
                conn.prepareStatement(sqlQueries.getSqlString("retrieveMessageSQL", true));
            retrieveMessage.setString(1, key);
            retrieveMessage.setString(2, repositoryName);
            ResultSet rsMessage = retrieveMessage.executeQuery();
            if (DEEP_DEBUG) {
                System.err.println("ran the query " + key);
            }
            if (!rsMessage.next()) {
                throw new RuntimeException("Did not find a record " + key + " in " + repositoryName);
            }
            MailImpl mc = new MailImpl();
            mc.setName(key);
            mc.setState(rsMessage.getString(1));
            mc.setErrorMessage(rsMessage.getString(2));
            String sender = rsMessage.getString(3);
            if (sender == null) {
                mc.setSender(null);
            } else {
                mc.setSender(new MailAddress(sender));
            }
            StringTokenizer st = new StringTokenizer(rsMessage.getString(4), "\r\n", false);
            Set recipients = new HashSet();
            while (st.hasMoreTokens()) {
                recipients.add(new MailAddress(st.nextToken()));
            }
            mc.setRecipients(recipients);
            mc.setRemoteHost(rsMessage.getString(5));
            mc.setRemoteAddr(rsMessage.getString(6));
            mc.setLastUpdated(rsMessage.getTimestamp(7));

            MimeMessageJDBCSource source = new MimeMessageJDBCSource(this, key, sr);
            MimeMessageWrapper message = new MimeMessageWrapper(source);
            mc.setMessage(message);
            rsMessage.close();
            retrieveMessage.close();
            return mc;
        } catch (SQLException sqle) {
            synchronized (System.err) {
                System.err.println("Error retrieving message");
                System.err.println(sqle.getMessage());
                System.err.println(sqle.getErrorCode());
                System.err.println(sqle.getSQLState());
                System.err.println(sqle.getNextException());
                sqle.printStackTrace();
            }
            throw new RuntimeException("Exception while retrieving mail: " + sqle.getMessage());
        } catch (Exception me) {
            me.printStackTrace();
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    //ignore
                }
            }
        }
    }

    public void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public void remove(String key) {
        //System.err.println("removing " + key);
        if (lock(key)) {
            Connection conn = null;
            try {
                conn = datasource.getConnection();
                PreparedStatement removeMessage =
                    conn.prepareStatement(sqlQueries.getSqlString("removeMessageSQL", true));
                removeMessage.setString(1, key);
                removeMessage.setString(2, repositoryName);
                removeMessage.execute();
                removeMessage.close();

                if (sr != null) {
                    sr.remove(key);
                }
            } catch (Exception me) {
                throw new RuntimeException("Exception while removing mail: " + me.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException sqle) {
                        //ignore
                    }
                }
                unlock(key);
            }
        }
    }

    public Iterator list() {
        //System.err.println("listing messages");
        Connection conn = null;
        try {
            conn = datasource.getConnection();
            PreparedStatement listMessages =
                conn.prepareStatement(sqlQueries.getSqlString("listMessagesSQL", true));
            listMessages.setString(1, repositoryName);
            ResultSet rsListMessages = listMessages.executeQuery();

            List messageList = new ArrayList();
            while (rsListMessages.next()) {
                messageList.add(rsListMessages.getString(1));
            }
            rsListMessages.close();
            listMessages.close();
            return messageList.iterator();
        } catch (Exception me) {
            me.printStackTrace();
            throw new RuntimeException("Exception while listing mail: " + me.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    //ignore
                }
            }
        }
    }

    protected Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof JDBCMailRepository)) {
            return false;
        }
        JDBCMailRepository repository = (JDBCMailRepository)obj;
        return repository.tableName.equals(tableName) && repository.repositoryName.equals(repositoryName);
    }
}
