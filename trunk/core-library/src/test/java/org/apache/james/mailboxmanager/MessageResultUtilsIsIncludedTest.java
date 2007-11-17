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

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import junit.framework.TestCase;

public class MessageResultUtilsIsIncludedTest extends MockObjectTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldReturnFalseWhenNull() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(null, MessageResult.FLAGS));
    }
   
    public void testBodyContentIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.BODY_CONTENT));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS), MessageResult.BODY_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.BODY_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.MSN | MessageResult.BODY_CONTENT), MessageResult.BODY_CONTENT));
    }
   
    public void testMsnIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.MSN));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS), MessageResult.MSN));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.MSN), MessageResult.MSN));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.MSN | MessageResult.BODY_CONTENT), MessageResult.MSN));
    }
    
    public void testFlagsIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.FLAGS));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.FLAGS));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS), MessageResult.FLAGS));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.BODY_CONTENT), MessageResult.FLAGS));
    }
    
    public void testFULL_CONTENTIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.FULL_CONTENT));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.FULL_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FULL_CONTENT), MessageResult.FULL_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.FULL_CONTENT), MessageResult.FULL_CONTENT));
    }
    
    public void testHEADERSIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.HEADERS));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.HEADERS));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.HEADERS), MessageResult.HEADERS));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.HEADERS), MessageResult.HEADERS));
    }
    
    public void testINTERNAL_DATEIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.INTERNAL_DATE));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.INTERNAL_DATE));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.INTERNAL_DATE), MessageResult.INTERNAL_DATE));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.INTERNAL_DATE), MessageResult.INTERNAL_DATE));
    }
    
    public void testMIME_MESSAGEIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.MIME_MESSAGE));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.MIME_MESSAGE));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.MIME_MESSAGE), MessageResult.MIME_MESSAGE));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.MIME_MESSAGE), MessageResult.MIME_MESSAGE));
    }
    
    public void testShouldNOTHINGAlwaysBeIncluded() throws Exception {
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING, false), MessageResult.NOTHING));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT, false), MessageResult.NOTHING));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.INTERNAL_DATE, false), MessageResult.NOTHING));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.MIME_MESSAGE, false), MessageResult.NOTHING));
    }
    
    public void testMultipleData() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.NOTHING), MessageResult.MIME_MESSAGE | MessageResult.FLAGS));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.BODY_CONTENT), MessageResult.MIME_MESSAGE | MessageResult.FLAGS));
        assertFalse(MessageResultUtils.isIncluded(mock(MessageResult.MIME_MESSAGE), MessageResult.MIME_MESSAGE | MessageResult.FLAGS));
        assertTrue(MessageResultUtils.isIncluded(mock(MessageResult.FLAGS | MessageResult.MIME_MESSAGE), MessageResult.MIME_MESSAGE | MessageResult.FLAGS));
    }
    
    private MessageResult mock(int included) {
        return mock(included, true);
    }
    
    private MessageResult mock(int included, boolean willBeCalled) {
        Mock result = mock(MessageResult.class);
        if (willBeCalled) {
            result.expects(once()).method("getIncludedResults").will(returnValue(included));
        }
        return (MessageResult) result.proxy();
    }
}
