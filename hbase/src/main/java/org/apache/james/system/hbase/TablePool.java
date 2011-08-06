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

public class TablePool {    
    
    private static Configuration configuration;
    private static TablePool hbaseSchema;
    private static HTablePool htablePool;
    
    public static TablePool getInstance() throws IOException {
        return getInstance(HBaseConfiguration.create());
    }

    public static TablePool getInstance(Configuration configuration) throws IOException {
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
    
    public HTable getDomainlistTable() {
        return (HTable) htablePool.getTable(HDomainList.TABLE_NAME);
    }

    public HTable getRecipientRewriteTable() {
        return (HTable) htablePool.getTable(HRecipientRewriteTable.TABLE_NAME);
    }

    public HTable getUsersRepositoryTable() {
        return (HTable) htablePool.getTable(HUsersRepository.TABLE_NAME);
    }

    // With later HBase versions, we won't have to put back the table in the pool.
    // See https://issues.apache.org/jira/browse/HBASE-4054
    public void putTable(HTable table) {
        if (table != null) {
            htablePool.putTable(table);
        }
    }
    
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
