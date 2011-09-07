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
package org.apache.james.system.hbase;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.james.domainlist.hbase.def.HDomainList;
import org.apache.james.rrt.hbase.def.HRecipientRewriteTable;
import org.apache.james.user.hbase.def.HUsersRepository;

/**
 * Table Pool singleton to get the DomainList, RecipientRewriteTable and UserRepository HBase tables.
 * 
 * TODO Two getInstance methods are public, one for the impl, one for the tests. This is not good.
 */
public class TablePool {    
    
    private static Configuration configuration;
    private static TablePool hbaseSchema;
    private static HTablePool htablePool;
    
    /**
     * Use getInstance to get an instance of the HTablePool.
     * 
     * Don't give any configuration, the default one will be used
     * via HBaseConfiguration.create().
     * 
     * If you want to create the instance with a specific HBase configuration,
     * use {@link #getInstance(Configuration)}
     * 
     * @return
     * @throws IOException
     */
    public static synchronized TablePool getInstance() throws IOException {
        return getInstance(HBaseConfiguration.create());
    }

    /**
     * Use getInstance to get an instance of the HTablePool.
     * 
     * You can give at first call a specific HBase configuration to suit your needs.
     * 
     * @param configuration
     * @return
     * @throws IOException
     */
    public static synchronized TablePool getInstance(Configuration configuration) throws IOException {
        if (hbaseSchema == null) {
            TablePool.configuration = configuration;
            TablePool.hbaseSchema = new TablePool();
            TablePool.htablePool = new HTablePool(configuration, 100);
            ensureTable(HDomainList.TABLE_NAME, HDomainList.COLUMN_FAMILY_NAME);
            ensureTable(HRecipientRewriteTable.TABLE_NAME, HRecipientRewriteTable.COLUMN_FAMILY_NAME);
            ensureTable(HUsersRepository.TABLE_NAME, HUsersRepository.COLUMN_FAMILY_NAME);
        }
        return hbaseSchema;
    }
    
    /**
     * Get an instance of the DomainList table.
     * 
     * @return
     */
    public HTable getDomainlistTable() {
        return (HTable) htablePool.getTable(HDomainList.TABLE_NAME);
    }

    /**
     * Get an instance of the RecipientRewriteTable table.
     * 
     * @return
     */
    public HTable getRecipientRewriteTable() {
        return (HTable) htablePool.getTable(HRecipientRewriteTable.TABLE_NAME);
    }

    /**
     * Get an instance of the UsersRepository table.
     * 
     * @return
     */
    public HTable getUsersRepositoryTable() {
        return (HTable) htablePool.getTable(HUsersRepository.TABLE_NAME);
    }

    /**
     * Put back the table in the pool after usage.
     * 
     * With later HBase versions, we won't have to put back the table in the pool.
     * See https://issues.apache.org/jira/browse/HBASE-4054
     * 
     * @param table
     */
    public void putTable(HTable table) {
        if (table != null) {
            htablePool.putTable(table);
        }
    }
    
    /**
     * Create a table if needed.
     * 
     * @param tableName
     * @param columnFamilyName
     * @throws IOException
     */
    private static void ensureTable(byte[] tableName, byte[] columnFamilyName) throws IOException {
        HBaseAdmin hbaseAdmin = new HBaseAdmin(configuration);
        if (! hbaseAdmin.tableExists(tableName)) {
          HTableDescriptor desc = new HTableDescriptor(tableName);
          HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(columnFamilyName);
          hColumnDescriptor.setMaxVersions(1);
          desc.addFamily(hColumnDescriptor);
          hbaseAdmin.createTable(desc);
        }
    }

}
