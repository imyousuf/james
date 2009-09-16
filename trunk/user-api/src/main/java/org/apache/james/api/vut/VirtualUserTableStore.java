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



package org.apache.james.api.vut;


/**
 * Contains VirtualUserTable. A VirtualUserTableStore
 * contains one or more VirtualUserTables. Multiple VirtualUserTables may or may
 * not have overlapping membership. 
 *
 */
public interface VirtualUserTableStore 
{
    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.api.vut.VirtualUserTableStore";

    /** 
     * Get the table, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the VirtualUserTable corresponding to the name parameter
     */
    VirtualUserTable getTable( String name );
}
