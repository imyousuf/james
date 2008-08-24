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
package org.apache.james.mailboxmanager.torque;

import java.util.List;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class TorqueMailboxManagerTest extends AbstractMailboxRowTestCase {
    

    
    public TorqueMailboxManagerTest() throws TorqueException {
        super();
    }

    public void testCreateRenameDeleteMailbox() throws TorqueException, MailboxManagerException {
        mm.createMailbox("#users.tuser.INBOX");
        List l=MailboxRowPeer.doSelect(new Criteria());
        assertEquals(1,l.size());
        assertEquals("#users.tuser.INBOX",((MailboxRow)l.get(0)).getName());
        
        mm.renameMailbox("#users.tuser.INBOX","#users.tuser2.INBOX");
        l=MailboxRowPeer.doSelect(new Criteria());
        assertEquals(1,l.size());
        assertEquals("#users.tuser2.INBOX",((MailboxRow)l.get(0)).getName());
        
        mm.deleteMailbox("#users.tuser2.INBOX", null);
        l=MailboxRowPeer.doSelect(new Criteria());
        assertEquals(0,l.size());
    }
}
