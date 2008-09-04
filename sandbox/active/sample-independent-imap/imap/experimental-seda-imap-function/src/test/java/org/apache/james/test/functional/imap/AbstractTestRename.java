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

package org.apache.james.test.functional.imap;

import java.util.Locale;

public abstract class AbstractTestRename extends AbstractTestSelectedStateBase {

    public AbstractTestRename(HostSystem system) {
        super(system);
    }

    public void testRenameUS() throws Exception {
        scriptTest("Rename", Locale.US);
    }
    
    public void testRenameKOREA() throws Exception {
        scriptTest("Rename", Locale.KOREA);
    }
    
    public void testRenameITALY() throws Exception {
        scriptTest("Rename", Locale.ITALY);
    }
    
    public void testRenameHierarchyUS() throws Exception {
        scriptTest("RenameHierarchy", Locale.US);
    }
    
    public void testRenameHierarchyKO() throws Exception {
        scriptTest("RenameHierarchy", Locale.KOREA);
    }
    
    public void testRenameHierarchyIT() throws Exception {
        scriptTest("RenameHierarchy", Locale.ITALY);
    }
    
    public void testRenameSelectedUS() throws Exception {
        scriptTest("RenameSelected", Locale.US);
    }
    
    public void testRenameSelectedIT() throws Exception {
        scriptTest("RenameSelected", Locale.ITALY);
    }
    
    public void testRenameSelectedKO() throws Exception {
        scriptTest("RenameSelected", Locale.KOREA);
    }
}
