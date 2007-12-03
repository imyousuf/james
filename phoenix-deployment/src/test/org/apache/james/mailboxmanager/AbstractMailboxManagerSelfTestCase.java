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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.mailboxmanager.manager.MailboxExpression;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

import junit.framework.TestCase;

public abstract class AbstractMailboxManagerSelfTestCase extends TestCase {
    
    protected MailboxManager mailboxManager;
    protected MailboxManagerProvider mailboxManagerProvider;
    
    public void testCreateList() throws MailboxManagerException {
        ListResult[] listResult;
        listResult=mailboxManager.list(new MailboxExpression("","*", '*', '%'));
        assertNotNull(listResult);
        assertEquals(0,mailboxManager.list(new MailboxExpression("","*", '*', '%')).length);
        Set boxes=new HashSet();
        boxes.add("#users.joachim.INBOX");
        boxes.add("#users.joachim.INBOX.Drafts");
        boxes.add("#users.joachim2.INBOX");
        for (Iterator iter = boxes.iterator(); iter.hasNext();) {
            String box = (String) iter.next();
            mailboxManager.createMailbox(box);    
        }
        listResult=mailboxManager.list(new MailboxExpression("","*", '*', '%'));
        assertEquals(3,listResult.length);
        for (int i = 0; i < listResult.length; i++) {
            assertTrue(boxes.contains(listResult[i].getName()));
        }
    }
    
    public void testListOne() throws MailboxManagerException {
        mailboxManager.createMailbox("test1");    
        mailboxManager.createMailbox("INBOX");
        mailboxManager.createMailbox("INBOX2");
        
        ListResult[] listResult=mailboxManager.list(new MailboxExpression("","*", '*', '%'));
        assertNotNull(listResult);
        assertEquals(1, listResult.length);
        assertEquals("INBOX", listResult[0].getName());
    }

}
