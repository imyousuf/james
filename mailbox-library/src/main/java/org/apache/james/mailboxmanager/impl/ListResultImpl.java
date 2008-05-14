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

package org.apache.james.mailboxmanager.impl;

import org.apache.james.mailboxmanager.ListResult;

public class ListResultImpl implements ListResult {

    public static ListResult createNoSelect(String name, String delimiter) {
        return new ListResultImpl(name, delimiter, false, SELECTABILITY_FLAG_NOSELECT);
    }

    
    private final String name;
    private final String delimiter;
    private final boolean noInferiors;
    private final int selectability;

    public ListResultImpl(String name, String delimiter) {
        this(name, delimiter, false, SELECTABILITY_FLAG_NONE);
    }
   
    public ListResultImpl(final String name, final String delimiter, final boolean noInferiors, 
            final int selectability) {
        super();
        this.name = name;
        this.delimiter = delimiter;
        this.noInferiors = noInferiors;
        this.selectability = selectability;
    }

    /**
     * Is this mailbox <code>\Noinferiors</code> as per RFC3501.
     * @return true if marked, false otherwise
     */
    public final boolean isNoInferiors() {
        return noInferiors;
    }

    /**
     * Gets the RFC3501 Selectability flag setting.
     * @return {@link ListResult#SELECTABILITY_FLAG_NONE},
     * {@link ListResult#SELECTABILITY_FLAG_MARKED},
     * {@link ListResult#SELECTABILITY_FLAG_NOSELECT},
     * or {@link ListResult#SELECTABILITY_FLAG_UNMARKED}
     */
    public final int getSelectability() {
        return selectability;
    }


    public String getHierarchyDelimiter() {
        return delimiter;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "ListResult: " + name;
    }
}
