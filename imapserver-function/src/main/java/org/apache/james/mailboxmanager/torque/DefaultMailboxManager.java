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

package org.apache.james.mailboxmanager.torque;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.james.mailboxmanager.torque.om.MessageBodyPeer;
import org.apache.james.mailboxmanager.torque.om.MessageFlagsPeer;
import org.apache.james.mailboxmanager.torque.om.MessageHeaderPeer;
import org.apache.james.mailboxmanager.torque.om.MessageRowPeer;
import org.apache.james.services.FileSystem;
import org.apache.james.util.sql.SqlResources;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Transaction;

public class DefaultMailboxManager extends TorqueMailboxManager implements Configurable, LogEnabled{

    private static final String[] tableNames = new String[] {
        MailboxRowPeer.TABLE_NAME, MessageRowPeer.TABLE_NAME,
        MessageHeaderPeer.TABLE_NAME, MessageBodyPeer.TABLE_NAME,
        MessageFlagsPeer.TABLE_NAME };
    
    private FileSystem fileSystem;
    private String configFile;

    private String torqueFile;
    
    public DefaultMailboxManager(Authenticator authenticator, Subscriber subscripters) {
        super(authenticator, subscripters);       
    }

    @Resource(name="filesystem")
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    @PostConstruct
    public void init() throws Exception {
        if (Torque.isInit()) {
            throw new RuntimeException("Torque is already initialized!");
        } 
        Connection conn = null;
        try {
            Torque.init(new PropertiesConfiguration(fileSystem.getFile(torqueFile)));
            conn = Transaction.begin(MailboxRowPeer.DATABASE_NAME);
            SqlResources sqlResources = new SqlResources();
            sqlResources.init(fileSystem.getResource(configFile),
                DefaultMailboxManager.class.getName(), conn,
                new HashMap<String,String>());

            DatabaseMetaData dbMetaData = conn.getMetaData();

            for (int i = 0; i < tableNames.length; i++) {
                if (!tableExists(dbMetaData, tableNames[i])) {
                    BasePeer.executeStatement(sqlResources
                            .getSqlString("createTable_" + tableNames[i]),
                            conn);
                    System.out.println("Created table " + tableNames[i]);
                    getLog().info("Created table " + tableNames[i]);
                }
            }

            Transaction.commit(conn);
            System.out.println("MailboxManager has been initialized");
            getLog().info("MailboxManager has been initialized");
        } catch (Exception e) {
            Transaction.safeRollback(conn);
            try {
                Torque.shutdown();
            } catch (TorqueException e1) {
                // ignore on shutdown
            }
            throw new MailboxException(new HumanReadableText("org.apache.james.imap.INIT_FAILED", "Initialisation failed"), e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration conf)
            throws ConfigurationException {
        configFile = conf.getString("configFile",null);
        if (configFile == null) configFile = "file://conf/mailboxManagerSqlResources.xml";
        torqueFile = conf.getString("torqueConfigFile",null);
        if (torqueFile == null) torqueFile = "file://conf/torque.properties";
    }

    private boolean tableExists(DatabaseMetaData dbMetaData, String tableName)
            throws SQLException {
        return (tableExistsCaseSensitive(dbMetaData, tableName)
                || tableExistsCaseSensitive(dbMetaData, tableName
                        .toUpperCase(Locale.US)) || tableExistsCaseSensitive(
                dbMetaData, tableName.toLowerCase(Locale.US)));
    }

    private boolean tableExistsCaseSensitive(DatabaseMetaData dbMetaData,
            String tableName) throws SQLException {
        ResultSet rsTables = dbMetaData.getTables(null, null, tableName, null);
        try {
            boolean found = rsTables.next();
            return found;
        } finally {
            if (rsTables != null) {
                rsTables.close();
            }
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }
}
