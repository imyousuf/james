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

package org.apache.james.mailboxmanager.torque.om;

import java.sql.SQLException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.james.mailboxmanager.torque.AbstractMailboxRowTestCase;
import org.apache.torque.TorqueException;

public class MailboxRowTest extends AbstractMailboxRowTestCase {

    public MailboxRowTest() throws TorqueException {
        super();
    }

    
    public void testConcurrentConsumeUid() throws Exception {
        MailboxRow mr=new MailboxRow("#users.tuser.INBOX",100);
        mr.save();
        mr=MailboxRowPeer.retrieveByName("#users.tuser.INBOX");
        ConsumeUidThread[] t=new ConsumeUidThread[10];
        for (int i = 0; i < t.length; i++) {
            t[i]=new ConsumeUidThread(mr);
            t[i].start();
        }
        SortedSet set=new TreeSet();
        for (int i = 0; i < t.length; i++) {
            t[i].join();
            set.add(new Long(t[i].mr.getLastUid()));
        }
        assertEquals(t.length,set.size());
        
    }
    
    class ConsumeUidThread extends Thread {
        MailboxRow mr;
        
        ConsumeUidThread( MailboxRow mr) {
            this.mr=mr;
        }
        public void run() {
            try {
                mr=mr.consumeNextUid();
            } catch (TorqueException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    public void testConsumeUid() throws Exception {
        MailboxRow mr=new MailboxRow("#users.tuser2.INBOX",100);
        mr.save();
        mr=MailboxRowPeer.retrieveByName("#users.tuser2.INBOX");
        assertEquals(0,mr.getLastUid());
        mr=mr.consumeNextUid();
        assertEquals(1,mr.getLastUid());
        mr=mr.consumeNextUid();
        assertEquals(2,mr.getLastUid());
        mr=mr.consumeNextUid();
        assertEquals(3,mr.getLastUid());
    }
    
}
