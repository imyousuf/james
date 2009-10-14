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



package org.apache.james.test.mock.james;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;

public class MockVirtualUserTableStore implements VirtualUserTableStore {
    public Map<String, VirtualUserTable> tableStore = new HashMap<String, VirtualUserTable>();
    
    public VirtualUserTable getTable(String name) {
        return (VirtualUserTable) tableStore.get(name);
    }


    public void addTable(String tableName) {
        tableStore.put(tableName, new MockVirtualUserTableManagementImpl());
    }
    
    public void removeTable(String tableName) {
        tableStore.remove(tableName);
    }
    
}
