/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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

package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.context.AvalonContextUtilities;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.services.MailRepository;
import org.apache.james.util.JDBCUtil;
import org.apache.james.util.Lock;
import org.apache.james.util.SqlResources;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Implementation of a MailRepository on a database.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br>&lt;repository destinationURL="db://&lt;datasource&gt;/&lt;table_name&gt;/&lt;repository_name&gt;"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/&gt;
 *  <br>&lt;/repository&gt;
 * <p>destinationURL specifies..(Serge??)
 * <br>Type can be SPOOL or MAIL
 * <br>Model is currently not used and may be dropped
 *
 * <p>Requires a logger called MailRepository.
 *
 * @version CVS $Revision$ $Date$
 */
public class JDBCMailRepository
    extends AbstractLogEnabled
    implements MailRepository, Contextualizable, Serviceable, Configurable, Initializable {

    /**
     * Whether 'deep debugging' is turned on.
     */
    private static final boolean DEEP_DEBUG = false;

    /**
     * The Avalon context used by the instance
     */
    protected Context context;

    /**
     * A lock used to control access to repository elements, locking access
     * based on the key 
     */
    private Lock lock;

    /**
     * The table name parsed from the destination URL
     */
    protected String tableName;

    /**
     * The repository name parsed from the destination URL
     */
    protected String repositoryName;

    /**
     * The name of the SQL configuration file to be used to configure this repository.
     */
    private String sqlFileName;

    /**
     * The stream repository used in dbfile mode
     */
    private StreamRepository sr = null;

    /**
     * The selector used to obtain the JDBC datasource
     */
    protected DataSourceSelector datasources;

    /**
     * The JDBC datasource that provides the JDBC connection
     */
    protected DataSourceComponent datasource;

    /**
     * The store where the repository is selected from
     */
    protected Store store; 
    
    /**
     * The name of the datasource used by this repository
     */
    protected String datasourceName;

    /**
     * Contains all of the sql strings for this component.
     */
    protected SqlResources sqlQueries;

    /**
     * The JDBCUtil helper class
     */
    protected JDBCUtil theJDBCUtil;
    
    /**
     * "Support for Mail Attributes under JDBC repositories is ready" indicator.
     */
    protected boolean jdbcMailAttributesReady = false;

    /**
     * The size threshold for in memory handling of storing operations
     */
    private int inMemorySizeLimit;

    public void setStore(Store store) {
        this.store = store;
    }

    public void setDatasources(DataSourceSelector datasources) {
        this.datasources = datasources;
    }

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        StringBuffer logBuffer = null;
        if (getLogger().isDebugEnabled()) {
            logBuffer =
                new StringBuffer(64)
                        .append(this.getClass().getName())
                        .append(".compose()");
            getLogger().debug(logBuffer.toString());
        }
        // Get the DataSourceSelector service
        DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup( DataSourceSelector.ROLE );
        setDatasources(datasources);
        Store store = (Store)componentManager.lookup(Store.ROLE);
        setStore(store);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(this.getClass().getName() + ".configure()");
        }

        String destination = conf.getAttribute("destinationURL");
        // normalize the destination, to simplify processing.
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
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Malformed destinationURL - Must be of the format '")
                        .append("db://<data-source>[/<table>[/<repositoryName>]]'.  Was passed ")
                        .append(conf.getAttribute("destinationURL"));
            throw new ConfigurationException(exceptionBuffer.toString());
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

        if (getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                        .append("Parsed URL: table = '")
                        .append(tableName)
                        .append("', repositoryName = '")
                        .append(repositoryName)
                        .append("'");
            getLogger().debug(logBuffer.toString());
        }
        
        inMemorySizeLimit = conf.getChild("inMemorySizeLimit").getValueAsInteger(409600000); 

        String filestore = conf.getChild("filestore").getValue(null);
        sqlFileName = conf.getChild("sqlFile").getValue();
        if (!sqlFileName.startsWith("file://")) {
            throw new ConfigurationException
                ("Malformed sqlFile - Must be of the format 'file://<filename>'.");
        }
        try {
            if (filestore != null) {
                //prepare Configurations for stream repositories
                DefaultConfiguration streamConfiguration
                    = new DefaultConfiguration( "repository",
                                                "generated:JDBCMailRepository.configure()" );

                streamConfiguration.setAttribute( "destinationURL", filestore );
                streamConfiguration.setAttribute( "type", "STREAM" );
                streamConfiguration.setAttribute( "model", "SYNCHRONOUS" );
                sr = (StreamRepository) store.select(streamConfiguration);

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Got filestore for JdbcMailRepository: " + filestore);
                }
            }

            lock = new Lock();
            if (getLogger().isDebugEnabled()) {
                StringBuffer logBuffer =
                    new StringBuffer(128)
                            .append(this.getClass().getName())
                            .append(" created according to ")
                            .append(destination);
                getLogger().debug(logBuffer.toString());
            }
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error(message, e);
            throw new ConfigurationException(message, e);
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
     * @throws Exception if an error occurs
     */
    public void initialize() throws Exception {
        StringBuffer logBuffer = null;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(this.getClass().getName() + ".initialize()");
        }

        theJDBCUtil =
            new JDBCUtil() {
                protected void delegatedLog(String logString) {
                    JDBCMailRepository.this.getLogger().warn("JDBCMailRepository: " + logString);
                }
            };
        // Get the data-source required.
        datasource = (DataSourceComponent)datasources.select(datasourceName);

        // Test the connection to the database, by getting the DatabaseMetaData.
        Connection conn = datasource.getConnection();
        PreparedStatement createStatement = null;

        try {
            // Initialise the sql strings.

            File sqlFile = null;
            try {
                sqlFile = AvalonContextUtilities.getFile(context, sqlFileName);
                sqlFileName = null;
            } catch (Exception e) {
                getLogger().fatalError(e.getMessage(), e);
                throw e;
            }

            if (getLogger().isDebugEnabled()) {
                logBuffer =
                    new StringBuffer(128)
                            .append("Reading SQL resources from file: ")
                            .append(sqlFile.getAbsolutePath())
                            .append(", section ")
                            .append(this.getClass().getName())
                            .append(".");
                getLogger().debug(logBuffer.toString());
            }

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
            if (!(theJDBCUtil.tableExists(dbMetaData, tableName))) {
                // Users table doesn't exist - create it.
                createStatement =
                    conn.prepareStatement(sqlQueries.getSqlString("createTable", true));
                createStatement.execute();

                if (getLogger().isInfoEnabled()) {
                    logBuffer =
                        new StringBuffer(64)
                                .append("JdbcMailRepository: Created table '")
                                .append(tableName)
                                .append("'.");
                    getLogger().info(logBuffer.toString());
                }
            }
            
            checkJdbcAttributesSupport(dbMetaData);

        } finally {
            theJDBCUtil.closeJDBCStatement(createStatement);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }
    
    /** Checks whether support for JDBC Mail atributes is activated for this repository
     * and if everything is consistent.
     * Looks for both the "updateMessageAttributesSQL" and "retrieveMessageAttributesSQL"
     * statements in sqlResources and for a table column named "message_attributes".
     *
     * @param dbMetaData the database metadata to be used to look up the column
     * @throws SQLException if a fatal situation is met
     */
    protected void checkJdbcAttributesSupport(DatabaseMetaData dbMetaData) throws SQLException {
        String attributesColumnName = "message_attributes";
        boolean hasUpdateMessageAttributesSQL = false;
        boolean hasRetrieveMessageAttributesSQL = false;
        
        boolean hasMessageAttributesColumn = theJDBCUtil.columnExists(dbMetaData, tableName, attributesColumnName);
        
        StringBuffer logBuffer = new StringBuffer(64)
                                    .append("JdbcMailRepository '"
                                            + repositoryName
                                            + ", table '"
                                            + tableName
                                            + "': ");
        
        //Determine whether attributes are used and available for storing
        //Do we have updateMessageAttributesSQL?
        String updateMessageAttrSql =
            sqlQueries.getSqlString("updateMessageAttributesSQL", false);
        if (updateMessageAttrSql!=null) {
            hasUpdateMessageAttributesSQL = true;
        }
        
        //Determine whether attributes are used and retrieve them
        //Do we have retrieveAttributesSQL?
        String retrieveMessageAttrSql =
            sqlQueries.getSqlString("retrieveMessageAttributesSQL", false);
        if (retrieveMessageAttrSql!=null) {
            hasRetrieveMessageAttributesSQL = true;
        }
        
        if (hasUpdateMessageAttributesSQL && !hasRetrieveMessageAttributesSQL) {
            logBuffer.append("JDBC Mail Attributes support was activated for update but not for retrieval"
                             + "(found 'updateMessageAttributesSQL' but not 'retrieveMessageAttributesSQL'"
                             + "in table '"
                             + tableName
                             + "').");
            getLogger().fatalError(logBuffer.toString());
            throw new SQLException(logBuffer.toString());
        }
        if (!hasUpdateMessageAttributesSQL && hasRetrieveMessageAttributesSQL) {
            logBuffer.append("JDBC Mail Attributes support was activated for retrieval but not for update"
                             + "(found 'retrieveMessageAttributesSQL' but not 'updateMessageAttributesSQL'"
                             + "in table '"
                             + tableName
                             + "'.");
            getLogger().fatalError(logBuffer.toString());
            throw new SQLException(logBuffer.toString());
        }
        if (!hasMessageAttributesColumn
            && (hasUpdateMessageAttributesSQL || hasRetrieveMessageAttributesSQL)
            ) {
                logBuffer.append("JDBC Mail Attributes support was activated but column '"
                                 + attributesColumnName
                                 + "' is missing in table '"
                                 + tableName
                                 + "'.");
                getLogger().fatalError(logBuffer.toString());
                throw new SQLException(logBuffer.toString());
        }
        if (hasUpdateMessageAttributesSQL && hasRetrieveMessageAttributesSQL) {
            jdbcMailAttributesReady = true;
            if (getLogger().isInfoEnabled()) {
                logBuffer.append("JDBC Mail Attributes support ready.");
                getLogger().info(logBuffer.toString());
            }
        } else {
            jdbcMailAttributesReady = false;
            logBuffer.append("JDBC Mail Attributes support not activated. "
                             + "Missing both 'updateMessageAttributesSQL' "
                             + "and 'retrieveMessageAttributesSQL' "
                             + "statements for table '"
                             + tableName
                             + "' in sqlResources.xml. "
                             + "Will not persist in the repository '"
                             + repositoryName
                             + "'.");
            getLogger().warn(logBuffer.toString());
        }
    }

    /**
     * Releases a lock on a message identified by a key
     *
     * @param key the key of the message to be unlocked
     *
     * @return true if successfully released the lock, false otherwise
     */
    public boolean unlock(String key) {
        if (lock.unlock(key)) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer debugBuffer =
                    new StringBuffer(256)
                            .append("Unlocked ")
                            .append(key)
                            .append(" for ")
                            .append(Thread.currentThread().getName())
                            .append(" @ ")
                            .append(new java.util.Date(System.currentTimeMillis()));
                getLogger().debug(debugBuffer.toString());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Obtains a lock on a message identified by a key
     *
     * @param key the key of the message to be locked
     *
     * @return true if successfully obtained the lock, false otherwise
     */
    public boolean lock(String key) {
        if (lock.lock(key)) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer debugBuffer =
                    new StringBuffer(256)
                            .append("Locked ")
                            .append(key)
                            .append(" for ")
                            .append(Thread.currentThread().getName())
                            .append(" @ ")
                            .append(new java.util.Date(System.currentTimeMillis()));
                getLogger().debug(debugBuffer.toString());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Store this message to the database.  Optionally stores the message
     * body to the filesystem and only writes the headers to the database.
     */
    public void store(Mail mc) throws MessagingException {
        Connection conn = null;
        boolean wasLocked = true;
        String key = mc.getName();
        try {
            synchronized(this) {
                  wasLocked = lock.isLocked(key);
    
                  if (!wasLocked) {
                      //If it wasn't locked, we want a lock during the store
                      lock(key);
                  }
            }
            conn = datasource.getConnection();

            //Need to determine whether need to insert this record, or update it.

            //Begin a transaction
            conn.setAutoCommit(false);

            PreparedStatement checkMessageExists = null;
            ResultSet rsExists = null;
            boolean exists = false;
            try {
                checkMessageExists = 
                    conn.prepareStatement(sqlQueries.getSqlString("checkMessageExistsSQL", true));
                checkMessageExists.setString(1, mc.getName());
                checkMessageExists.setString(2, repositoryName);
                rsExists = checkMessageExists.executeQuery();
                exists = rsExists.next() && rsExists.getInt(1) > 0;
            } finally {
                theJDBCUtil.closeJDBCResultSet(rsExists);
                theJDBCUtil.closeJDBCStatement(checkMessageExists);
            }

            if (exists) {
                //Update the existing record
                PreparedStatement updateMessage = null;

                try {
                    updateMessage =
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
                } finally {
                    Statement localUpdateMessage = updateMessage;
                    // Clear reference to statement
                    updateMessage = null;
                    theJDBCUtil.closeJDBCStatement(localUpdateMessage);
                }

                //Determine whether attributes are used and available for storing
                if (jdbcMailAttributesReady && mc.hasAttributes()) {
                    String updateMessageAttrSql =
                        sqlQueries.getSqlString("updateMessageAttributesSQL", false);
                    PreparedStatement updateMessageAttr = null;
                    try {
                        updateMessageAttr =
                            conn.prepareStatement(updateMessageAttrSql);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        try {
                            if (mc instanceof MailImpl) {
                            oos.writeObject(((MailImpl)mc).getAttributesRaw());
                            } else {
                                HashMap temp = new HashMap();
                                for (Iterator i = mc.getAttributeNames(); i.hasNext(); ) {
                                    String hashKey = (String) i.next();
                                    temp.put(hashKey,mc.getAttribute(hashKey));
                                }
                                oos.writeObject(temp);
                            }
                            oos.flush();
                            ByteArrayInputStream attrInputStream =
                                new ByteArrayInputStream(baos.toByteArray());
                            updateMessageAttr.setBinaryStream(1, attrInputStream, baos.size());
                        } finally {
                            try {
                                if (oos != null) {
                                    oos.close();
                                }
                            } catch (IOException ioe) {
                                getLogger().debug("JDBCMailRepository: Unexpected exception while closing output stream.",ioe);
                            }
                        }
                        updateMessageAttr.setString(2, mc.getName());
                        updateMessageAttr.setString(3, repositoryName);
                        updateMessageAttr.execute();
                    } catch (SQLException sqle) {
                        getLogger().info("JDBCMailRepository: Trying to update mail attributes failed.",sqle);
                        
                    } finally {
                        theJDBCUtil.closeJDBCStatement(updateMessageAttr);
                    }
                }

                //Determine whether the message body has changed, and possibly avoid
                //  updating the database.
                MimeMessage messageBody = mc.getMessage();
                boolean saveBody = false;
                // if the message is a CopyOnWrite proxy we check the modified wrapped object.
                if (messageBody instanceof MimeMessageCopyOnWriteProxy) {
                    MimeMessageCopyOnWriteProxy messageCow = (MimeMessageCopyOnWriteProxy) messageBody;
                    messageBody = messageCow.getWrappedMessage();
                }
                if (messageBody instanceof MimeMessageWrapper) {
                    MimeMessageWrapper message = (MimeMessageWrapper)messageBody;
                    saveBody = message.isModified();
                } else {
                    saveBody = true;
                }
                
                if (saveBody) {
                    PreparedStatement updateMessageBody = 
                        conn.prepareStatement(sqlQueries.getSqlString("updateMessageBodySQL", true));
                    try {
                        MessageInputStream is = new MessageInputStream(mc,sr,inMemorySizeLimit);
                        updateMessageBody.setBinaryStream(1,is,(int) is.getSize());
                        updateMessageBody.setString(2, mc.getName());
                        updateMessageBody.setString(3, repositoryName);
                        updateMessageBody.execute();
                        
                    } finally {
                        theJDBCUtil.closeJDBCStatement(updateMessageBody);
                    }
                }
                

            } else {
                //Insert the record into the database
                PreparedStatement insertMessage = null;
                try {
                    String insertMessageSQL = sqlQueries.getSqlString("insertMessageSQL", true);
                    int number_of_parameters = getNumberOfParameters (insertMessageSQL);
                    insertMessage =
                        conn.prepareStatement(insertMessageSQL);
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

                    MessageInputStream is = new MessageInputStream(mc, sr, inMemorySizeLimit);

                    insertMessage.setBinaryStream(10, is, (int) is.getSize());
                    
                    //Store attributes
                    if (number_of_parameters > 10) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        try {
                            if (mc instanceof MailImpl) {
                            oos.writeObject(((MailImpl)mc).getAttributesRaw());
                            } else {
                                HashMap temp = new HashMap();
                                for (Iterator i = mc.getAttributeNames(); i.hasNext(); ) {
                                    String hashKey = (String) i.next();
                                    temp.put(hashKey,mc.getAttribute(hashKey));
                                }
                                oos.writeObject(temp);
                            }
                            oos.flush();
                            ByteArrayInputStream attrInputStream =
                                new ByteArrayInputStream(baos.toByteArray());
                            insertMessage.setBinaryStream(11, attrInputStream, baos.size());
                        } finally {
                            try {
                                if (oos != null) {
                                    oos.close();
                                }
                            } catch (IOException ioe) {
                                getLogger().debug("JDBCMailRepository: Unexpected exception while closing output stream.",ioe);
                            }
                        }                        
                    }
                    
                    insertMessage.execute();
                } finally {
                    theJDBCUtil.closeJDBCStatement(insertMessage);
                }
            }


            conn.commit();
            conn.setAutoCommit(true);

        } catch (Exception e) {
            getLogger().error("Exception caught while storing mail Container",e);
            throw new MessagingException("Exception caught while storing mail Container: ",e);
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
            if (!wasLocked) {
                // If it wasn't locked, we need to unlock now
                unlock(key);
                synchronized (this) {
                    notify();
                }
            }
        }
    }

    /**
     * Retrieves a message given a key. At the moment, keys can be obtained
     * from list()
     *
     * @param key the key of the message to retrieve
     * @return the mail corresponding to this key, null if none exists
     */
    public Mail retrieve(String key) throws MessagingException {
        if (DEEP_DEBUG) {
            System.err.println("retrieving " + key);
        }
        Connection conn = null;
        PreparedStatement retrieveMessage = null;
        ResultSet rsMessage = null;
        try {
            conn = datasource.getConnection();
            if (DEEP_DEBUG) {
                System.err.println("got a conn " + key);
            }

            retrieveMessage =
                conn.prepareStatement(sqlQueries.getSqlString("retrieveMessageSQL", true));
            retrieveMessage.setString(1, key);
            retrieveMessage.setString(2, repositoryName);
            rsMessage = retrieveMessage.executeQuery();
            if (DEEP_DEBUG) {
                System.err.println("ran the query " + key);
            }
            if (!rsMessage.next()) {
                if (getLogger().isDebugEnabled()) {
                    StringBuffer debugBuffer =
                        new StringBuffer(64)
                                .append("Did not find a record ")
                                .append(key)
                                .append(" in ")
                                .append(repositoryName);
                    getLogger().debug(debugBuffer.toString());
                }
                return null;
            }
            //Determine whether attributes are used and retrieve them
            PreparedStatement retrieveMessageAttr = null;
            HashMap attributes = null;
            if (jdbcMailAttributesReady) {
                String retrieveMessageAttrSql =
                    sqlQueries.getSqlString("retrieveMessageAttributesSQL", false);
                ResultSet rsMessageAttr = null;
                try {
                    retrieveMessageAttr =
                        conn.prepareStatement(retrieveMessageAttrSql);
                    
                    retrieveMessageAttr.setString(1, key);
                    retrieveMessageAttr.setString(2, repositoryName);
                    rsMessageAttr = retrieveMessageAttr.executeQuery();
                    
                    if (rsMessageAttr.next()) {
                        try {
                            byte[] serialized_attr = null;
                            String getAttributesOption = sqlQueries.getDbOption("getAttributes");
                            if (getAttributesOption != null && (getAttributesOption.equalsIgnoreCase("useBlob") || getAttributesOption.equalsIgnoreCase("useBinaryStream"))) {
                                Blob b = rsMessageAttr.getBlob(1);
                                serialized_attr = b.getBytes(1, (int)b.length());
                            } else {
                                serialized_attr = rsMessageAttr.getBytes(1);
                            }
                            // this check is for better backwards compatibility
                            if (serialized_attr != null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(serialized_attr);
                                ObjectInputStream ois = new ObjectInputStream(bais);
                                attributes = (HashMap)ois.readObject();
                                ois.close();
                            }
                        } catch (IOException ioe) {
                            if (getLogger().isDebugEnabled()) {
                                StringBuffer debugBuffer =
                                    new StringBuffer(64)
                                    .append("Exception reading attributes ")
                                    .append(key)
                                    .append(" in ")
                                    .append(repositoryName);
                                getLogger().debug(debugBuffer.toString(), ioe);
                            }
                        }
                    } else {
                        if (getLogger().isDebugEnabled()) {
                            StringBuffer debugBuffer =
                                new StringBuffer(64)
                                .append("Did not find a record (attributes) ")
                                .append(key)
                                .append(" in ")
                            .append(repositoryName);
                            getLogger().debug(debugBuffer.toString());
                        }
                    }
                } catch (SQLException sqle) {
                    StringBuffer errorBuffer =  new StringBuffer(256)
                                                .append("Error retrieving message")
                                                .append(sqle.getMessage())
                                                .append(sqle.getErrorCode())
                                                .append(sqle.getSQLState())
                                                .append(sqle.getNextException());
                    getLogger().error(errorBuffer.toString());
                } finally {
                    theJDBCUtil.closeJDBCResultSet(rsMessageAttr);
                    theJDBCUtil.closeJDBCStatement(retrieveMessageAttr);
                }
            }

            MailImpl mc = new MailImpl();
            mc.setAttributesRaw (attributes);
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
            MimeMessageCopyOnWriteProxy message = new MimeMessageCopyOnWriteProxy(source);
            mc.setMessage(message);
            return mc;
        } catch (SQLException sqle) {
            StringBuffer errorBuffer =  new StringBuffer(256)
                                        .append("Error retrieving message")
                                        .append(sqle.getMessage())
                                        .append(sqle.getErrorCode())
                                        .append(sqle.getSQLState())
                                        .append(sqle.getNextException());
            getLogger().error(errorBuffer.toString());
            throw new MessagingException("Exception while retrieving mail: " + sqle.getMessage());
        } catch (Exception me) {
            throw new MessagingException("Exception while retrieving mail: " + me.getMessage());
        } finally {
            theJDBCUtil.closeJDBCResultSet(rsMessage);
            theJDBCUtil.closeJDBCStatement(retrieveMessage);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Removes a specified message
     *
     * @param mail the message to be removed from the repository
     */
    public void remove(Mail mail) throws MessagingException {
        remove(mail.getName());
    }

    /**
     * Removes a Collection of mails from the repository
     * @param mails The Collection of <code>MailImpl</code>'s to delete
     * @throws MessagingException
     * @since 2.2.0
     */
    public void remove(Collection mails) throws MessagingException {
        Iterator delList = mails.iterator();
        while (delList.hasNext()) {
            remove((Mail)delList.next());
        }
    }

    /**
     * Removes a message identified by a key.
     *
     * @param key the key of the message to be removed from the repository
     */
    public void remove(String key) throws MessagingException {
        //System.err.println("removing " + key);
        if (lock(key)) {
            Connection conn = null;
            PreparedStatement removeMessage = null;
            try {
                conn = datasource.getConnection();
                removeMessage =
                    conn.prepareStatement(sqlQueries.getSqlString("removeMessageSQL", true));
                removeMessage.setString(1, key);
                removeMessage.setString(2, repositoryName);
                removeMessage.execute();

                if (sr != null) {
                    sr.remove(key);
                }
            } catch (Exception me) {
                throw new MessagingException("Exception while removing mail: " + me.getMessage());
            } finally {
                theJDBCUtil.closeJDBCStatement(removeMessage);
                theJDBCUtil.closeJDBCConnection(conn);
                unlock(key);
            }
        }
    }

    /**
     * Gets a list of message keys stored in this repository.
     *
     * @return an Iterator of the message keys
     */
    public Iterator list() throws MessagingException {
        //System.err.println("listing messages");
        Connection conn = null;
        PreparedStatement listMessages = null;
        ResultSet rsListMessages = null;
        try {
            conn = datasource.getConnection();
            listMessages =
                conn.prepareStatement(sqlQueries.getSqlString("listMessagesSQL", true));
            listMessages.setString(1, repositoryName);
            rsListMessages = listMessages.executeQuery();

            List messageList = new ArrayList();
            while (rsListMessages.next() && !Thread.currentThread().isInterrupted()) {
                messageList.add(rsListMessages.getString(1));
            }
            return messageList.iterator();
        } catch (Exception me) {
            throw new MessagingException("Exception while listing mail: " + me.getMessage());
        } finally {
            theJDBCUtil.closeJDBCResultSet(rsListMessages);
            theJDBCUtil.closeJDBCStatement(listMessages);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Gets the SQL connection to be used by this JDBCMailRepository
     *
     * @return the connection
     * @throws SQLException if there is an issue with getting the connection
     */
    protected Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof JDBCMailRepository)) {
            return false;
        }
        // TODO: Figure out whether other instance variables should be part of
        // the equals equation
        JDBCMailRepository repository = (JDBCMailRepository)obj;
        return  ((repository.tableName == tableName) || ((repository.tableName != null) && repository.tableName.equals(tableName))) && 
                ((repository.repositoryName == repositoryName) || ((repository.repositoryName != null) && repository.repositoryName.equals(repositoryName)));
    }

    /**
     * Provide a hash code that is consistent with equals for this class
     *
     * @return the hash code
     */
     public int hashCode() {
        int result = 17;
        if (tableName != null) {
            result = 37 * tableName.hashCode();
        }
        if (repositoryName != null) {
            result = 37 * repositoryName.hashCode();
        }
        return result;
     }

    /**
     * This method calculates number of parameters in a prepared statement SQL String.
     * It does so by counting the number of '?' in the string 
     * @param sqlstring to return parameter count for
     * @return number of parameters
     **/
    private int getNumberOfParameters (String sqlstring) {
        //it is alas a java 1.4 feature to be able to call
        //getParameterMetaData which could provide us with the parameterCount
        char[] chars = sqlstring.toCharArray();
        int count = 0;
        for (int i = 0; i < chars.length; i++) {
            count += chars[i]=='?' ? 1 : 0;
        }
        return count;
    }
}
