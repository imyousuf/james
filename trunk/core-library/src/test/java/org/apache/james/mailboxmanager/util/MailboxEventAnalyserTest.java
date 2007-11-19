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

import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MockMailboxListenerAdded;
import org.apache.james.mailboxmanager.MockMailboxListenerFlagsUpdate;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class MailboxEventAnalyserTest extends MockObjectTestCase {

    private static final long BASE_SESSION_ID = 99;
    
    MailboxEventAnalyser analyser;
    
    protected void setUp() throws Exception {
        super.setUp();
        analyser = new MailboxEventAnalyser(BASE_SESSION_ID);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldBeNoSizeChangeOnOtherEvent() throws Exception {
        analyser.event((MailboxListener.Event) mock(MailboxListener.Event.class).proxy());
        assertFalse(analyser.isSizeChanged());
    }
    
    public void testShouldBeNoSizeChangeOnAdded() throws Exception {
        analyser.event(new MockMailboxListenerAdded((MessageResult) mock(MessageResult.class).proxy(), 11));
        assertTrue(analyser.isSizeChanged());
    }
    
    public void testShouldNoSizeChangeAfterReset() throws Exception {
        analyser.event(new MockMailboxListenerAdded((MessageResult) mock(MessageResult.class).proxy(), 11));
        analyser.reset();
        assertFalse(analyser.isSizeChanged());
    }
    
    public void testShouldNotSetUidWhenNoSystemFlagChange() throws Exception {
        final MockMailboxListenerFlagsUpdate update = new MockMailboxListenerFlagsUpdate((MessageResult) mock(MessageResult.class).proxy(), 11);
        analyser.event(update);
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().hasNext());
    }
    
    public void testShouldSetUidWhenSystemFlagChange() throws Exception {
        final Mock mockMessageResult = mock(MessageResult.class);
        final long uid = 900L;
        mockMessageResult.expects(once()).method("getUid").will(returnValue(uid));
        final MockMailboxListenerFlagsUpdate update = new MockMailboxListenerFlagsUpdate((MessageResult) mockMessageResult.proxy(), 11);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(new Long(uid), iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testShouldClearFlagUidsUponReset() throws Exception {
        final Mock mockMessageResult = mock(MessageResult.class);
        final long uid = 900L;
        mockMessageResult.expects(once()).method("getUid").will(returnValue(uid));
        final MockMailboxListenerFlagsUpdate update = new MockMailboxListenerFlagsUpdate((MessageResult) mockMessageResult.proxy(), 11);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.event(update);
        analyser.reset();
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().hasNext());
    }
    
    public void testShouldNotSetUidWhenSystemFlagChangeDifferentSessionInSilentMode() throws Exception {
        final Mock mockMessageResult = mock(MessageResult.class);
        final long uid = 900L;
        mockMessageResult.expects(once()).method("getUid").will(returnValue(uid));
        final MockMailboxListenerFlagsUpdate update = new MockMailboxListenerFlagsUpdate((MessageResult) mockMessageResult.proxy(), 11);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(new Long(uid), iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testShouldNotSetUidWhenSystemFlagChangeSameSessionInSilentMode() throws Exception {
        final Mock mockMessageResult = mock(MessageResult.class);
        final MockMailboxListenerFlagsUpdate update = new MockMailboxListenerFlagsUpdate((MessageResult) mockMessageResult.proxy(), BASE_SESSION_ID);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }
    
    public void testShouldNotSetUidWhenOnlyRecentFlagUpdated() throws Exception {
        final Mock mockMessageResult = mock(MessageResult.class);
        final MockMailboxListenerFlagsUpdate update = new MockMailboxListenerFlagsUpdate((MessageResult) mockMessageResult.proxy(), BASE_SESSION_ID);
        update.flags.add(Flags.Flag.RECENT);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext()); 
    }
}
