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
import java.util.Date;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.jmock.MockObjectTestCase;

public class MessageResultImplIncludedResultsTest extends MockObjectTestCase {

    MessageResultImpl result;
    MessageResult.Content content;
    
    protected void setUp() throws Exception {
        super.setUp();
        result = new MessageResultImpl();
        content = (MessageResult.Content) mock(MessageResult.Content.class).proxy();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testShouldIncludedResultsWhenMimeMessageSet() throws Exception {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        result.setMimeMessage(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        result.setMimeMessage(message);
        assertEquals(MessageResult.MIME_MESSAGE, result.getIncludedResults());
        MessageResultImpl result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.MIME_MESSAGE, result.getIncludedResults());
    }

    public void testShouldIncludedResultsWhenFlagsSet() throws Exception {
        result.setFlags(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        Flags flags = new Flags();
        result.setFlags(flags);
        assertEquals(MessageResult.FLAGS, result.getIncludedResults());
        MessageResultImpl result = new MessageResultImpl(77, flags);
        assertEquals(MessageResult.FLAGS, result.getIncludedResults());
        result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.FLAGS, result.getIncludedResults());
    }

    public void testShouldIncludedResultsWhenSizeSet() throws Exception {
        result.setSize(100);
        assertEquals(MessageResult.SIZE, result.getIncludedResults());
        MessageResultImpl result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.SIZE, result.getIncludedResults());
    }

    public void testShouldIncludedResultsWhenInternalDateSet() throws Exception {
        result.setInternalDate(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        Date date = new Date();
        result.setInternalDate(date);
        assertEquals(MessageResult.INTERNAL_DATE, result.getIncludedResults());
        result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.INTERNAL_DATE, result.getIncludedResults());
    }

    public void testShouldIncludedResultsWhenKeySet() throws Exception {
        result.setKey(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        result.setKey("KEY");
        assertEquals(MessageResult.KEY, result.getIncludedResults());
        result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.KEY, result.getIncludedResults());
    }

    public void testShouldIncludedResultsWhenHeadersSet() throws Exception {
        result.setHeaders(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        result.setHeaders(new ArrayList());
        assertEquals(MessageResult.HEADERS, result.getIncludedResults());
        result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.HEADERS, result.getIncludedResults());
    }
    
    public void testShouldIncludedResultsWhenFullMessageSet() throws Exception  {
        result.setFullMessage(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        result.setFullMessage(content);
        assertEquals(MessageResult.FULL_CONTENT, result.getIncludedResults());
        result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.FULL_CONTENT, result.getIncludedResults());
    }

    public void testShouldIncludedResultsWhenMessageBodySet() throws Exception {
        result.setMessageBody(null);
        assertEquals(MessageResult.MINIMAL, result.getIncludedResults());
        result.setMessageBody(content);
        assertEquals(MessageResult.BODY_CONTENT, result.getIncludedResults());
        result = new MessageResultImpl(this.result);
        assertEquals(MessageResult.BODY_CONTENT, result.getIncludedResults());
    }
    
    public void testShouldIncludedResultsWhenAllSet() {
        Flags flags = new Flags();
        result.setFlags(flags);
        assertEquals(MessageResult.FLAGS, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        result.setUid(99);
        assertEquals(MessageResult.FLAGS, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        result.setMessageBody(content);
        assertEquals(MessageResult.FLAGS | MessageResult.BODY_CONTENT, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        result.setFullMessage(content);
        assertEquals(MessageResult.FLAGS | 
                MessageResult.BODY_CONTENT | MessageResult.FULL_CONTENT, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        result.setHeaders(new ArrayList());
        assertEquals(MessageResult.FLAGS | 
                MessageResult.BODY_CONTENT | MessageResult.FULL_CONTENT 
                | MessageResult.HEADERS, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        assertTrue(MessageResultUtils.isHeadersIncluded(result));
        result.setInternalDate(new Date());
        assertEquals(MessageResult.FLAGS | 
                MessageResult.BODY_CONTENT | MessageResult.FULL_CONTENT 
                | MessageResult.HEADERS | MessageResult.INTERNAL_DATE, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        assertTrue(MessageResultUtils.isHeadersIncluded(result));
        assertTrue(MessageResultUtils.isInternalDateIncluded(result));
        result.setSize(100);
        assertEquals(MessageResult.FLAGS | 
                MessageResult.BODY_CONTENT | MessageResult.FULL_CONTENT 
                | MessageResult.HEADERS | MessageResult.INTERNAL_DATE
                | MessageResult.SIZE, result.getIncludedResults());
        assertTrue(MessageResultUtils.isFlagsIncluded(result));
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        assertTrue(MessageResultUtils.isHeadersIncluded(result));
        assertTrue(MessageResultUtils.isInternalDateIncluded(result));
        assertTrue(MessageResultUtils.isSizeIncluded(result));
    }
}
