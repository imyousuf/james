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

package org.apache.james.api.imap.process;

import java.util.List;

public interface SelectedImapMailbox {

    public abstract void deselect();

    public abstract List unsolicitedResponses(boolean omitExpunged,
            boolean useUid);
    
    public int msn(long uid);

    public abstract long uid(int i);
    
    public boolean addRecent(long uid);
    
    public boolean removeRecent(long uid);
    
    public long[] getRecent();
    
    public int recentCount();
    
    public String getName();

    public boolean isRecent(long uid);
    
    /**
     * Is the mailbox deleted?
     * @return true when the mailbox has been deleted by another session, 
     * false otherwise
     */
    public boolean isDeletedByOtherSession();
}