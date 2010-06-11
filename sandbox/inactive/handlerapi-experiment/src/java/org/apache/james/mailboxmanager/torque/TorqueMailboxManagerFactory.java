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

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.james.mailboxmanager.torque.om.MessageBodyPeer;
import org.apache.james.mailboxmanager.torque.om.MessageFlagsPeer;
import org.apache.james.mailboxmanager.torque.om.MessageHeaderPeer;
import org.apache.james.mailboxmanager.torque.om.MessageRowPeer;
import org.apache.james.mailboxmanager.tracking.MailboxCache;
import org.apache.james.services.FileSystem;
import org.apache.james.services.User;
import org.apache.james.util.SqlResources;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Transaction;

public class TorqueMailboxManagerFactory implements MailboxManagerFactory,
        Configurable, Initializable, Serviceable, LogEnabled {

    private MailboxCache mailboxCache;

    private BaseConfiguration torqueConf;

    private boolean initialized;

    private Log log;

    private FileSystem fileSystem;

    private static final String[] tableNames = new String[] {
            MailboxRowPeer.TABLE_NAME, MessageRowPeer.TABLE_NAME,
            MessageHeaderPeer.TABLE_NAME, MessageBodyPeer.TABLE_NAME,
            MessageFlagsPeer.TABLE_NAME };

    public MailboxManager getMailboxManagerInstance(User user)
            throws MailboxManagerException {
        if (!initialized) {
            throw new MailboxManagerException("must be initialized first!");
        }
        return new TorqueMailboxManager(user, getMailboxCache(), getLog());
    }

    public void initialize() throws Exception {
        if (!initialized) {
            if (torqueConf == null) {
                throw new RuntimeException("must be configured first!");
            }
            if (Torque.isInit()) {
                throw new RuntimeException("Torque is already initialized!");
            }
            Connection conn = null;
            try {
                Torque.init(torqueConf);
                conn = Transaction.begin(MailboxRowPeer.DATABASE_NAME);
                SqlResources sqlResources = new SqlResources();
                sqlResources.init(fileSystem
                        .getFile("file://conf/mailboxManagerSqlResources.xml"),
                        TorqueMailboxManagerFactory.class.getName(), conn,
                        new HashMap());

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
                initialized = true;
                System.out.println("MailboxManager has been initialized");
                getLog().info("MailboxManager has been initialized");
            } catch (Exception e) {
                System.err
                        .println("============================================");
                e.printStackTrace();
                System.err
                        .println("--------------------------------------------");
                Transaction.safeRollback(conn);
                try {
                    Torque.shutdown();
                } catch (TorqueException e1) {

                }
                throw new MailboxManagerException(e);
            }
        }
    }

    public void configureDefaults()
            throws org.apache.commons.configuration.ConfigurationException {
        File configFile = new File("torque.properties");
        if (configFile.canRead()) {
            getLog().info("reading torque.properties...");
            torqueConf = new PropertiesConfiguration(configFile);
        } else {
            torqueConf = new BaseConfiguration();
            torqueConf.addProperty("torque.database.default", "mailboxmanager");
            torqueConf.addProperty("torque.database.mailboxmanager.adapter",
                    "derby");
            torqueConf.addProperty("torque.dsfactory.mailboxmanager.factory",
                    "org.apache.torque.dsfactory.SharedPoolDataSourceFactory");
            torqueConf.addProperty(
                    "torque.dsfactory.mailboxmanager.connection.driver",
                    "org.apache.derby.jdbc.EmbeddedDriver");
            torqueConf.addProperty(
                    "torque.dsfactory.mailboxmanager.connection.url",
                    "jdbc:derby:target/testdb;create=true");
            torqueConf.addProperty(
                    "torque.dsfactory.mailboxmanager.connection.user", "app");
            torqueConf.addProperty(
                    "torque.dsfactory.mailboxmanager.connection.password",
                    "app");
            torqueConf.addProperty(
                    "torque.dsfactory.mailboxmanager.pool.maxActive", "100");
        }
    }

    public void configure(
            org.apache.avalon.framework.configuration.Configuration conf)
            throws ConfigurationException {
        torqueConf = new BaseConfiguration();
        org.apache.avalon.framework.configuration.Configuration[] tps = conf
                .getChild("torque-properties").getChildren("property");
        for (int i = 0; i < tps.length; i++) {
            torqueConf.addProperty(tps[i].getAttribute("name"), tps[i]
                    .getAttribute("value"));
        }
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

    private MailboxCache getMailboxCache() {
        if (mailboxCache == null) {
            mailboxCache = new MailboxCache();
        }
        return mailboxCache;
    }

    public void deleteEverything() throws MailboxManagerException {
        ((TorqueMailboxManager) getMailboxManagerInstance(null))
                .deleteEverything();
        mailboxCache = null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String toString() {
        return "TorqueMailboxManagerProvider";
    }

    protected Log getLog() {
        if (log == null) {
            log = new SimpleLog("TorqueMailboxManagerFactory");
        }
        return log;
    }

    public void enableLogging(Logger logger) {
        log = new AvalonLogger(logger);

    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        setFileSystem((FileSystem) serviceManager.lookup(FileSystem.ROLE));
    }

    protected void setFileSystem(FileSystem system) {
        this.fileSystem = system;
    }

    public void addMountPoint(String point) {
        // TODO Auto-generated method stub

    }

    public Map getOpenMailboxSessionCountMap() {
        return getMailboxCache().getOpenMailboxSessionCountMap();
    }

}
