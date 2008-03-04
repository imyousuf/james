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

import org.apache.james.mailboxmanager.SearchQuery;
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
    
    private void addHeader(String fieldName, String value) throws TorqueException {
        final MessageHeader messageHeader = new MessageHeader();
        messageHeader.setField(fieldName);
        messageHeader.setValue(value);
        row.addMessageHeader(messageHeader);
    }
}
