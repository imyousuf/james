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

package org.apache.james.imap.message.response.imap4rev1.server;

import java.util.Arrays;

import org.apache.james.api.imap.message.response.ImapResponseMessage;

/**
 * A <code>SEARCH</code> response.
 */
public class SearchResponse implements ImapResponseMessage {
    private final long ids[];

    /**
     * Constructs a <code>SEARCH</code> response.
     * @param ids ids, not null
     */
    public SearchResponse(final long[] ids) {
        super();
        this.ids = ids;
    }

    /**
     * Gets the ids returned by this search.
     * @return the ids, not null
     */
    public final long[] getIds() {
        return ids;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    //@Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + Arrays.hashCode(ids);
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    //@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SearchResponse other = (SearchResponse) obj;
        if (!Arrays.equals(ids, other.ids))
            return false;
        return true;
    }

    /**
     * Constructs a <code>String</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String TAB = " ";
        
        StringBuffer retValue = new StringBuffer();
        
        retValue.append("SearchResponse ( ")
            .append("ids = ").append(this.ids).append(TAB)
            .append(" )");
        
        return retValue.toString();
    }
}
