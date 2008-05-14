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

import java.util.Arrays;

import javax.mail.Flags;

import junit.framework.TestCase;

import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.util.UidChangeTracker;
import org.apache.james.mailboxmanager.util.UidRange;

public class UidChangeTrackerTestCase extends TestCase {
    
    protected UidChangeTracker tracker;
    
    protected MailboxListenerCollector collector;
    
    public void setUp() {
        tracker=new UidChangeTracker(1000);
        collector=new MailboxListenerCollector();
        tracker.addMailboxListener(collector);
    }
    
    
    
    protected void assertCollectorSizes(int added, int expunged, int flags) {
        assertEquals(added,collector.getAddedList(false).size());
        assertEquals(expunged,collector.getExpungedList(false).size());
        assertEquals(flags,collector.getFlaggedList(false).size());
    }
    
    
    public void testFound() throws Exception {
        MessageResultImpl[] results;
        MessageResult result;
        
        results=new MessageResultImpl[1];
        
        results[0]=new MessageResultImpl(1000l);
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));
        assertCollectorSizes(0,0,0);
        
        results[0]=new MessageResultImpl(1001l);
        tracker.found(new UidRange(1001,1001),Arrays.asList(results));
        assertCollectorSizes(1,0,0);
        assertEquals(1001,((MessageResult) collector.getAddedList(true).get(0)).getUid());
        assertCollectorSizes(0,0,0);
        
        results[0]=new MessageResultImpl(1001l,new Flags(Flags.Flag.FLAGGED));
        tracker.found(new UidRange(1001,1001),Arrays.asList(results));
        assertCollectorSizes(0,0,1);
        MessageFlags messageFlags =(MessageFlags) collector.getFlaggedList(true).get(0);
        assertEquals(1001,messageFlags.getUid());
        assertEquals(new Flags(Flags.Flag.FLAGGED),messageFlags.getFlags());
        
        // nothing changed
        tracker.found(new UidRange(1001,1001),Arrays.asList(results));
        assertCollectorSizes(0,0,0);
        
        // 1000 got expunged
        tracker.found(new UidRange(1000,1001),Arrays.asList(results));
        assertCollectorSizes(0,1,0);
        assertEquals(new Long(1000),collector.getExpungedList(true).get(0));
        
        
    }

    public void testShouldNotIssueFlagsUpdateEventWhenFlagsNotIncluded() throws Exception {
        MessageResultImpl[] results = new MessageResultImpl[1];
        
        MessageResult result;
        results[0]=new MessageResultImpl(1000l,new Flags(Flags.Flag.FLAGGED));
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));
        assertCollectorSizes(0,0,0);
        
        results[0]=new MessageResultImpl(1000l);
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));
        assertCollectorSizes(0,0,0);

        results[0]=new MessageResultImpl(1000l,new Flags(Flags.Flag.FLAGGED));
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));

        results[0]=new MessageResultImpl(1000l,new Flags(Flags.Flag.SEEN));
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));
        assertCollectorSizes(0,0,1);
        MessageFlags messageFlags =(MessageFlags) collector.getFlaggedList(true).get(0);
        assertEquals(1000,messageFlags.getUid());
        assertEquals(new Flags(Flags.Flag.SEEN),messageFlags.getFlags());
        
        results[0]=new MessageResultImpl(1000l);
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));
        assertCollectorSizes(0,0,0);
        
        results[0]=new MessageResultImpl(1000l,new Flags(Flags.Flag.DRAFT));
        tracker.found(new UidRange(1000,1000),Arrays.asList(results));
        assertCollectorSizes(0,0,1);
        result=(MessageResult) collector.getFlaggedList(true).get(0);
        assertEquals(1000,result.getUid());
        assertEquals(new Flags(Flags.Flag.DRAFT),result.getFlags());
        
    }
}
