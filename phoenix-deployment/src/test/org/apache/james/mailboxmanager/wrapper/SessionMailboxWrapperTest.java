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

package org.apache.james.mailboxmanager.wrapper;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsInstanceOf;

public class SessionMailboxWrapperTest extends MockObjectTestCase {

    public void testAddsAndRemovesListener() throws MailboxManagerException {
        
        SessionMailboxWrapper sessionMailboxWrapper = new SessionMailboxWrapper();
        
        Mock generalMailboxMock = mock(GeneralMailbox.class);

        final MailboxListener listenerObject = sessionMailboxWrapper.getListenerObject();
        generalMailboxMock.expects(once()).method("addListener").with(same(listenerObject));
        
        Constraint[] getMessagesArgs={new IsInstanceOf(GeneralMessageSet.class),new IsInstanceOf(Integer.class)};
        
        MessageResult[] result={new MessageResultImpl()};
        
        generalMailboxMock.expects(once()).method("getMessages").with(getMessagesArgs).after("addListener").will(returnValue(result));
        generalMailboxMock.expects(once()).method("removeListener").with(same(listenerObject)).after("getMessages");
        
        sessionMailboxWrapper.setMailbox((GeneralMailbox) generalMailboxMock
                .proxy());
        sessionMailboxWrapper.init();
        sessionMailboxWrapper.close();
    }

}
