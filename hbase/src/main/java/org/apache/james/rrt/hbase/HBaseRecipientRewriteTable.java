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
package org.apache.james.rrt.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.hbase.def.HRecipientRewriteTable;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTable;
import org.apache.james.system.hbase.TablePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseRecipientRewriteTable extends AbstractRecipientRewriteTable {

    private static Logger log = LoggerFactory.getLogger(HBaseRecipientRewriteTable.class.getName());
    
    @Override
    protected void addMappingInternal(String user, String domain, String mapping) throws RecipientRewriteTableException {
        HTable table = null;
        try {
            table = TablePool.getInstance().getRecipientRewriteTable();
            Put put = new Put(Bytes.toBytes(mapping));
            put.add(HRecipientRewriteTable.COLUMN_FAMILY_NAME, Bytes.toBytes(user), Bytes.toBytes(domain));
            table.put(put);
            table.flushCommits();
        } catch (IOException e) {
            log.error("Error while adding mapping in HBase", e);
            throw new RecipientRewriteTableException("Error while adding mapping in HBase", e);
        } finally {
            if (table != null) {
                try {
                    TablePool.getInstance().putTable(table);
                } catch (IOException e) {
                    // Do nothing, we can't get access to the HBaseSchema.
                }
            }
        }
    }

    @Override
    protected void removeMappingInternal(String user, String domain, String mapping) throws RecipientRewriteTableException {
        HTable table = null;
        try {
            table = TablePool.getInstance().getRecipientRewriteTable();
            Delete delete = new Delete(Bytes.toBytes(mapping));
            delete.deleteColumn(HRecipientRewriteTable.COLUMN_FAMILY_NAME, Bytes.toBytes(user));
            table.delete(delete);
            table.flushCommits();
        } catch (IOException e) {
            log.error("Error while removing mapping from HBase", e);
            throw new RecipientRewriteTableException("Error while removing mapping from HBase", e);
        } finally {
            if (table != null) {
                try {
                    TablePool.getInstance().putTable(table);
                } catch (IOException e) {
                    // Do nothing, we can't get access to the HBaseSchema.
                }
            }
        }
    }

    @Override
    protected Collection<String> getUserDomainMappingsInternal(String user, String domain) throws RecipientRewriteTableException {
        HTable table = null;
        List<String> list = new ArrayList<String>();
        ResultScanner resultScanner = null;
        try {
            table = TablePool.getInstance().getRecipientRewriteTable();
            Scan scan = new Scan();
            scan.addFamily(HRecipientRewriteTable.COLUMN_FAMILY_NAME);
            scan.setCaching(table.getScannerCaching() * 2);
            Filter filter = new SingleColumnValueFilter(HRecipientRewriteTable.COLUMN_FAMILY_NAME, Bytes.toBytes(user), CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes(user)));
            scan.setFilter(filter);
            resultScanner = table.getScanner(scan);
            Result result = null;
            while ((result = resultScanner.next()) != null) {
                list.add(Bytes.toString(result.getRow()));
            }
        } catch (IOException e) {
            log.error("Error while getting user domain mapping in HBase", e);
            throw new RecipientRewriteTableException("Error while getting user domain mapping in HBase", e);
        } finally {
            if (resultScanner != null) {
                resultScanner.close();
            }
            if (table != null) {
                try {
                    TablePool.getInstance().putTable(table);
                } catch (IOException e) {
                    // Do nothing, we can't get access to the HBaseSchema.
                }
            }
        }
        return list;
    }

    @Override
    protected Map<String, Collection<String>> getAllMappingsInternal() throws RecipientRewriteTableException {
        HTable table = null;
        ResultScanner resultScanner = null;
        Map<String, Collection<String>> map = null;
        try {
            table = TablePool.getInstance().getRecipientRewriteTable();
            Scan scan = new Scan();
            scan.addFamily(HRecipientRewriteTable.COLUMN_FAMILY_NAME);
            scan.setCaching(table.getScannerCaching() * 2);
            resultScanner = table.getScanner(scan);
            Result result = null;
            while ((result = resultScanner.next()) != null) {
                List<KeyValue> keyValues = result.list();
                if (keyValues != null) {
                    for (KeyValue keyValue: keyValues) {
                        byte[] user = keyValue.getQualifier();
                        byte[] domain = keyValue.getValue();
                        String email = Bytes.toString(user) + '@' + Bytes.toString(domain);
                        if (map == null) {
                            map = new HashMap<String, Collection<String>>();
                        }
                        Collection<String> list = map.get(email);
                        if (list == null) {
                            list = new ArrayList<String>();
                        }
                        list.add(Bytes.toString(keyValue.getRow()));
                        map.put(email, list);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error while getting all mapping from HBase", e);
            throw new RecipientRewriteTableException("Error while getting all mappings from HBase", e);
        } finally {
            if (resultScanner != null) {
                resultScanner.close();
            }
            if (table != null) {
                try {
                    TablePool.getInstance().putTable(table);
                } catch (IOException e) {
                    // Do nothing, we can't get access to the HBaseSchema.
                }
            }
        }
        return map;
    }

    @Override
    protected String mapAddressInternal(String user, String domain) throws RecipientRewriteTableException {
        HTable table = null;
        ResultScanner resultScanner = null;
        StringBuffer mappingBuffer = new StringBuffer();
        try {
            table = TablePool.getInstance().getRecipientRewriteTable();
            Scan scan = new Scan();
            scan.addFamily(HRecipientRewriteTable.COLUMN_FAMILY_NAME);
            scan.setCaching(table.getScannerCaching() * 2);
            Filter filter = new SingleColumnValueFilter(HRecipientRewriteTable.COLUMN_FAMILY_NAME, Bytes.toBytes(user), CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes(domain)));
            scan.setFilter(filter);
            resultScanner = table.getScanner(scan);
            Result result = null;
            while ((result = resultScanner.next()) != null) {
                mappingBuffer.append(Bytes.toString(result.getRow()) + ";");
            }
        } catch (IOException e) {
            log.error("Error while mapping address in HBase", e);
            throw new RecipientRewriteTableException("Error while mapping address in HBase", e);
        } finally {
            if (resultScanner != null) {
                resultScanner.close();
            }
            if (table != null) {
                try {
                    TablePool.getInstance().putTable(table);
                } catch (IOException e) {
                    // Do nothing, we can't get access to the HBaseSchema.
                }
            }
        }
        if (mappingBuffer.length() == 0) {
            return null;
        }
        String mapping = mappingBuffer.toString();
        return mapping.substring(0, mapping.length() - 1);
    }
    
}
