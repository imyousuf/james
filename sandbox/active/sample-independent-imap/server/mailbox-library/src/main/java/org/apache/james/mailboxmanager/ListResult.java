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

package org.apache.james.mailboxmanager;

/**
 * Returned by the list method of MailboxRepository and others
 */
public interface ListResult {
    
    /** Indicates that no RFC3501 Selectability flag is set */
    public static final int SELECTABILITY_FLAG_NONE = 0;
    /** Indicates that RFC3501 Selectability is set to \Marked */
    public static final int SELECTABILITY_FLAG_MARKED = 1;
    /** Indicates that RFC3501 Selectability is set to \Unmarked */
    public static final int SELECTABILITY_FLAG_UNMARKED = 2;
    /** Indicates that RFC3501 Selectability is set to \Noselect */
    public static final int SELECTABILITY_FLAG_NOSELECT = 3;
    
    public static final ListResult[] EMPTY_ARRAY = {};
    
    /**
     * Is this mailbox <code>\Noinferiors</code> as per RFC3501.
     * @return true if marked, false otherwise
     */
    public boolean isNoInferiors();

    /**
     * Gets the RFC3501 Selectability flag setting.
     * @return {@link #SELECTABILITY_FLAG_NONE},
     * {@link #SELECTABILITY_FLAG_MARKED},
     * {@link #SELECTABILITY_FLAG_NOSELECT},
     * or {@link #SELECTABILITY_FLAG_UNMARKED}
     */
    public int getSelectability();
    
    String getHierarchyDelimiter();
    
    /**
     * @return full namespace-name
     */
    String getName();
}
