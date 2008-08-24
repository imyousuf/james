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

package org.apache.james.mailboxmanager.util;

public class UidRange implements Comparable {
    
    private long fromUid;
    
    private long toUid;
    
    public UidRange(long fromUid,long toUid) {
        this.fromUid=fromUid;
        this.toUid=toUid;
    }

    public long getFromUid() {
        return fromUid;
    }

    public long getToUid() {
        return toUid;
    }
    
    public String toString() {
        return fromUid+":"+toUid;
    }

    public int compareTo(Object o) {
        if (!(o instanceof UidRange)) {
            return 1;
        } else {
            UidRange that = (UidRange) o;
            return new Long(fromUid).compareTo(new Long(that.fromUid));
        }
    }

}
