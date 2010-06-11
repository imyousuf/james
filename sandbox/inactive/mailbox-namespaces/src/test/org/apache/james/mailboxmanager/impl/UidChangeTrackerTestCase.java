package org.apache.james.mailboxmanager.impl;

import javax.mail.Flags;

import junit.framework.TestCase;

import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.tracking.UidChangeTracker;
import org.apache.james.mailboxmanager.tracking.UidRange;

public class UidChangeTrackerTestCase extends TestCase {
    
    protected UidChangeTracker tracker;
    
    protected MailboxListenerCollector collector;
    
    public void setUp() {
        tracker=new UidChangeTracker(null,"test",1000);
        collector=new MailboxListenerCollector();
        tracker.addMailboxListener(collector);
    }
    
    
    
    protected void assertCollectorSizes(int added, int expunged, int flags) {
        assertEquals(added,collector.getAddedList(false).size());
        assertEquals(expunged,collector.getExpungedList(false).size());
        assertEquals(flags,collector.getFlaggedList(false).size());
    }
    
    
    public void testFound() {
        MessageResultImpl[] results;
        MessageResult result;
        
        results=new MessageResultImpl[1];
        
        results[0]=new MessageResultImpl(1000l);
        tracker.found(new UidRange(1000,1000),results, null);
        assertCollectorSizes(0,0,0);
        
        results[0]=new MessageResultImpl(1001l);
        tracker.found(new UidRange(1001,1001),results, null);
        assertCollectorSizes(1,0,0);
        assertEquals(1001,((MessageResult) collector.getAddedList(true).get(0)).getUid());
        assertCollectorSizes(0,0,0);
        
        results[0]=new MessageResultImpl(1001l,new Flags(Flags.Flag.FLAGGED));
        tracker.found(new UidRange(1001,1001),results, null);
        assertCollectorSizes(0,0,1);
        result=(MessageResult) collector.getFlaggedList(true).get(0);
        assertEquals(1001,result.getUid());
        assertEquals(new Flags(Flags.Flag.FLAGGED),result.getFlags());
        
        // nothing changed
        tracker.found(new UidRange(1001,1001),results,null);
        assertCollectorSizes(0,0,0);
        
        // 1000 got expunged
        tracker.found(new UidRange(1000,1001),results,null);
        assertCollectorSizes(0,1,0);
        assertEquals(1000,((MessageResult) collector.getExpungedList(true).get(0)).getUid());
        
        
    }

}
