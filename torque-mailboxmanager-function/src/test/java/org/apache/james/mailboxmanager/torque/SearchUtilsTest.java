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

import java.util.Date;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.torque.om.MessageFlags;
import org.apache.james.mailboxmanager.torque.om.MessageHeader;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.mailet.RFC2822Headers;
import org.apache.torque.TorqueException;

import junit.framework.TestCase;

public class SearchUtilsTest extends TestCase {

    private static final String RHUBARD = "Rhubard";
    private static final String CUSTARD = "Custard";
    private static final Date SUN_SEP_9TH_2001 = new Date(1000000000000L);
    private static final int SIZE = 1729;
    private static final String DATE_FIELD = RFC2822Headers.DATE;
    private static final String SUBJECT_FIELD = RFC2822Headers.SUBJECT;
    private static final String RFC822_SUN_SEP_9TH_2001 = "Sun, 9 Sep 2001 09:10:48 +0000 (GMT)";
    private static final String TEXT = RHUBARD + RHUBARD + RHUBARD;
    
    MessageRow row;
    
    protected void setUp() throws Exception {
        super.setUp();
        row = new MessageRow();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMatchSizeLessThan() throws Exception {
        row.setSize(SIZE);
        assertFalse(SearchUtils.matches(SearchQuery.sizeLessThan(SIZE - 1), row));
        assertFalse(SearchUtils.matches(SearchQuery.sizeLessThan(SIZE), row));
        assertTrue(SearchUtils.matches(SearchQuery.sizeLessThan(SIZE + 1), row));
        assertTrue(SearchUtils.matches(SearchQuery.sizeLessThan(Integer.MAX_VALUE), row));
    }
    
    public void testMatchSizeMoreThan() throws Exception {
        row.setSize(SIZE);
        assertTrue(SearchUtils.matches(SearchQuery.sizeGreaterThan(SIZE - 1), row));
        assertFalse(SearchUtils.matches(SearchQuery.sizeGreaterThan(SIZE), row));
        assertFalse(SearchUtils.matches(SearchQuery.sizeGreaterThan(SIZE + 1), row));
        assertFalse(SearchUtils.matches(SearchQuery.sizeGreaterThan(Integer.MAX_VALUE), row));
    }
    
    public void testMatchSizeEquals() throws Exception {
        row.setSize(SIZE);
        assertFalse(SearchUtils.matches(SearchQuery.sizeEquals(SIZE - 1), row));
        assertTrue(SearchUtils.matches(SearchQuery.sizeEquals(SIZE), row));
        assertFalse(SearchUtils.matches(SearchQuery.sizeEquals(SIZE + 1), row));
        assertFalse(SearchUtils.matches(SearchQuery.sizeEquals(Integer.MAX_VALUE), row));
    }
    
    public void testMatchInternalDateEquals() throws Exception {
        row.setInternalDate(SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.internalDateOn(9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateOn(8, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.internalDateOn(9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateOn(10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateOn(9, 9, 2002), row));
    }
    
    public void testMatchInternalDateBefore() throws Exception {
        row.setInternalDate(SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.internalDateBefore(9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateBefore(8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateBefore(9, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.internalDateBefore(10, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.internalDateBefore(9, 9, 2002), row));
    }
        
    public void testMatchInternalDateAfter() throws Exception {
        row.setInternalDate(SUN_SEP_9TH_2001);
        assertTrue(SearchUtils.matches(SearchQuery.internalDateAfter(9, 9, 2000), row));
        assertTrue(SearchUtils.matches(SearchQuery.internalDateAfter(8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateAfter(9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateAfter(10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.internalDateAfter(9, 9, 2002), row));
    }
    
    public void testMatchHeaderDateAfter() throws Exception {
        addHeader(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        assertTrue(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD, 9, 9, 2000), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter("BOGUS",9, 9, 2001), row));
    }
    
    public void testShouldMatchCapsHeaderDateAfter() throws Exception {
        addHeader(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        assertTrue(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD, 9, 9, 2000), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter("BOGUS",9, 9, 2001), row));
    }
    
    public void testShouldMatchLowersHeaderDateAfter() throws Exception {
        addHeader(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        assertTrue(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD, 9, 9, 2000), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateAfter("BOGUS",9, 9, 2001), row));
    }
    
    public void testMatchHeaderDateOn() throws Exception {
        addHeader(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD, 9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,8, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn("BOGUS",9, 9, 2001), row));
    }
    
    public void testShouldMatchCapsHeaderDateOn() throws Exception {
        addHeader(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD, 9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,8, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn("BOGUS",9, 9, 2001), row));
     }
    
    public void testShouldMatchLowersHeaderDateOn() throws Exception {
        addHeader(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD, 9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,8, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,9, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,10, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateOn("BOGUS",9, 9, 2001), row));
    }
    
    
    public void testMatchHeaderDateBefore() throws Exception {
        addHeader(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD, 9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,9, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,10, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore("BOGUS",9, 9, 2001), row));
    }
    
    public void testShouldMatchCapsHeaderDateBefore() throws Exception {
        addHeader(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD, 9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,9, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,10, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore("BOGUS",9, 9, 2001), row));
    }
    
    public void testShouldMatchLowersHeaderDateBefore() throws Exception {
        addHeader(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD, 9, 9, 2000), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,8, 9, 2001), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,9, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,10, 9, 2001), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerDateBefore(DATE_FIELD,9, 9, 2002), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerDateBefore("BOGUS",9, 9, 2001), row));
    }
    
    public void testMatchHeaderContainsCaps() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT.toUpperCase());
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, CUSTARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, RHUBARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, CUSTARD), row)); 
    }
    
    public void testMatchHeaderContainsLowers() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT.toLowerCase());
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, CUSTARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, RHUBARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, CUSTARD), row)); 
    }
    
    public void testMatchHeaderContains() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT);
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, CUSTARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, RHUBARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, CUSTARD), row)); 
    }
    
    public void testShouldMatchLowerHeaderContains() throws Exception {
        addHeader(SUBJECT_FIELD.toLowerCase(), TEXT);
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, CUSTARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, RHUBARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, CUSTARD), row)); 
    }
    
    public void testShouldMatchCapsHeaderContains() throws Exception {
        addHeader(SUBJECT_FIELD.toUpperCase(), TEXT);
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, CUSTARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(DATE_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, TEXT), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, RHUBARD), row));
        assertFalse(SearchUtils.matches(SearchQuery.headerContains(SUBJECT_FIELD, CUSTARD), row)); 
    }
    
    public void testMatchHeaderExists() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT);
        assertFalse(SearchUtils.matches(SearchQuery.headerExists(DATE_FIELD), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerExists(SUBJECT_FIELD), row));
    }

    public void testShouldMatchLowersHeaderExists() throws Exception {
        addHeader(SUBJECT_FIELD.toLowerCase(), TEXT);
        assertFalse(SearchUtils.matches(SearchQuery.headerExists(DATE_FIELD), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerExists(SUBJECT_FIELD), row));
    }
    
    public void testShouldMatchUppersHeaderExists() throws Exception {
        addHeader(SUBJECT_FIELD.toUpperCase(), TEXT);
        assertFalse(SearchUtils.matches(SearchQuery.headerExists(DATE_FIELD), row));
        assertTrue(SearchUtils.matches(SearchQuery.headerExists(SUBJECT_FIELD), row));
    }
    
    public void testShouldMatchUidRange() throws Exception {
        row.setPrimaryKey(1, 1729);
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1, 1)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1728, 1728)), row));
        assertTrue(SearchUtils.matches(SearchQuery.uid(range(1729, 1729)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1730, 1730)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1, 1728)), row));
        assertTrue(SearchUtils.matches(SearchQuery.uid(range(1, 1729)), row));
        assertTrue(SearchUtils.matches(SearchQuery.uid(range(1729, 1800)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1730, Long.MAX_VALUE)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1730, Long.MAX_VALUE, 1, 1728)), row));
        assertTrue(SearchUtils.matches(SearchQuery.uid(range(1730, Long.MAX_VALUE, 1, 1729)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1, 1728, 1800, 1810)), row));
        assertTrue(SearchUtils.matches(SearchQuery.uid(range(1, 1, 1729, 1729)), row));
        assertFalse(SearchUtils.matches(SearchQuery.uid(range(1, 1, 1800, 1800)), row));
    }
    
    public void testShouldMatchSeenFlagSet() throws Exception {
        setFlags(true, false, false, false, false, false);
        assertTrue(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchAnsweredFlagSet() throws Exception {
        setFlags(false, false, true, false, false, false);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.FLAGGED), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchFlaggedFlagSet() throws Exception {
        setFlags(false, true, false, false, false, false);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.SEEN), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchDraftFlagSet() throws Exception {
        setFlags(false, false, false, true, false, false);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchDeletedFlagSet() throws Exception {
        setFlags(false, false, false, false, true, false);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DRAFT), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchSeenRecentSet() throws Exception {
        setFlags(false, false, false, false, false, true);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.DELETED), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsSet(Flags.Flag.RECENT), row));
    }

    public void testShouldMatchSeenFlagUnSet() throws Exception {
        setFlags(false, true, true, true, true, true);
        assertTrue(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchAnsweredFlagUnSet() throws Exception {
        setFlags(true, true, false, true, true, true);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchFlaggedFlagUnSet() throws Exception {
        setFlags(true, false, true, true, true, true);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.SEEN), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchDraftFlagUnSet() throws Exception {
        setFlags(true, true, true, false, true, true);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.ANSWERED), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchDeletedFlagUnSet() throws Exception {
        setFlags(true, true, true, true, false, true);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchSeenRecentUnSet() throws Exception {
        setFlags(true, true, true, true, true, false);
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.SEEN), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.ANSWERED), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT), row));
        assertFalse(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row));
        assertTrue(SearchUtils.matches(SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row));
    }
    
    public void testShouldMatchAll() throws Exception {
        assertTrue(SearchUtils.matches(SearchQuery.all(), row));
    }
    
    public void testShouldMatchNot() throws Exception {
        assertFalse(SearchUtils.matches(SearchQuery.not(SearchQuery.all()), row));
        assertTrue(SearchUtils.matches(SearchQuery.not(SearchQuery.headerExists(DATE_FIELD)), row));
    }
    
    public void testShouldMatchOr() throws Exception {
        assertTrue(SearchUtils.matches(SearchQuery.or(SearchQuery.all(), SearchQuery.headerExists(DATE_FIELD)), row));
        assertTrue(SearchUtils.matches(SearchQuery.or(SearchQuery.headerExists(DATE_FIELD), SearchQuery.all()), row));
        assertFalse(SearchUtils.matches(SearchQuery.or(SearchQuery.headerExists(DATE_FIELD), SearchQuery.headerExists(DATE_FIELD)), row));
        assertTrue(SearchUtils.matches(SearchQuery.or(SearchQuery.all(), SearchQuery.all()), row));
    }
    
    public void testShouldMatchAnd() throws Exception {
        assertFalse(SearchUtils.matches(SearchQuery.and(SearchQuery.all(), SearchQuery.headerExists(DATE_FIELD)), row));
        assertFalse(SearchUtils.matches(SearchQuery.and(SearchQuery.headerExists(DATE_FIELD), SearchQuery.all()), row));
        assertFalse(SearchUtils.matches(SearchQuery.and(SearchQuery.headerExists(DATE_FIELD), SearchQuery.headerExists(DATE_FIELD)), row));
        assertTrue(SearchUtils.matches(SearchQuery.and(SearchQuery.all(), SearchQuery.all()), row));
    }
    
    private void setFlags(boolean seen, boolean flagged, boolean answered, boolean draft, 
            boolean deleted, boolean recent) throws TorqueException {
        final MessageFlags messageFlags = new MessageFlags();
        messageFlags.setSeen(seen);
        messageFlags.setFlagged(flagged);
        messageFlags.setAnswered(answered);
        messageFlags.setDraft(draft);
        messageFlags.setDeleted(deleted);
        messageFlags.setRecent(recent);
        row.addMessageFlags(messageFlags);
    }
    
    private SearchQuery.NumericRange[] range(long low, long high) {
        SearchQuery.NumericRange[] results = {new SearchQuery.NumericRange(low, high)};
        return results;
    }
    
    private SearchQuery.NumericRange[] range(long lowOne, long highOne, long lowTwo, long highTwo) {
        SearchQuery.NumericRange[] results = {new SearchQuery.NumericRange(lowOne, highOne), new SearchQuery.NumericRange(lowTwo, highTwo)};
        return results;
    }
    
    private void addHeader(String fieldName, String value) throws TorqueException {
        final MessageHeader messageHeader = new MessageHeader();
        messageHeader.setField(fieldName);
        messageHeader.setValue(value);
        row.addMessageHeader(messageHeader);
    }
}
