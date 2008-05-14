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

import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.util.MessageResultUtils;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class MessageResultUtilsIsIncludedTest extends MockObjectTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldReturnFalseWhenNull() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(null, FetchGroup.FLAGS));
    }
   
    public void testBodyContentIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.BODY_CONTENT));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.FLAGS), FetchGroup.BODY_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.BODY_CONTENT));
    }
   
    public void testFlagsIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.FLAGS));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.FLAGS));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.FLAGS), FetchGroup.FLAGS));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.BODY_CONTENT)), FetchGroup.FLAGS));
    }
    
    public void testFULL_CONTENTIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.FULL_CONTENT));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.FULL_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.FULL_CONTENT), FetchGroup.FULL_CONTENT));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.FULL_CONTENT)), FetchGroup.FULL_CONTENT));
    }
    
    public void testHEADERSIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.HEADERS));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.HEADERS));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.HEADERS), FetchGroup.HEADERS));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.HEADERS)), FetchGroup.HEADERS));
    }
    
    public void testINTERNAL_DATEIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.INTERNAL_DATE));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.INTERNAL_DATE));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.INTERNAL_DATE), FetchGroup.INTERNAL_DATE));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.INTERNAL_DATE)), FetchGroup.INTERNAL_DATE));
    }
    
    public void testMIME_MESSAGEIncluded() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.MIME_MESSAGE));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.MIME_MESSAGE));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MIME_MESSAGE), FetchGroup.MIME_MESSAGE));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.MIME_MESSAGE)), FetchGroup.MIME_MESSAGE));
    }
    
    public void testShouldNOTHINGAlwaysBeIncluded() throws Exception {
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL, false), FetchGroup.MINIMAL));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT, false), FetchGroup.MINIMAL));
        assertTrue(MessageResultUtils.isIncluded(mock(FetchGroupImpl.INTERNAL_DATE, false), FetchGroup.MINIMAL));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.MIME_MESSAGE), false), FetchGroup.MINIMAL));
    }
    
    public void testMultipleData() throws Exception {
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MINIMAL), FetchGroup.MIME_MESSAGE | FetchGroup.FLAGS));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.BODY_CONTENT), FetchGroup.MIME_MESSAGE | FetchGroup.FLAGS));
        assertFalse(MessageResultUtils.isIncluded(mock(FetchGroupImpl.MIME_MESSAGE), FetchGroup.MIME_MESSAGE | FetchGroup.FLAGS));
        assertTrue(MessageResultUtils.isIncluded(mock(new FetchGroupImpl(FetchGroup.FLAGS | FetchGroup.MIME_MESSAGE)), FetchGroup.MIME_MESSAGE | FetchGroup.FLAGS));
    }
    
    private MessageResult mock(FetchGroup included) {
        return mock(included, true);
    }
    
    private MessageResult mock(FetchGroup included, boolean willBeCalled) {
        Mock result = mock(MessageResult.class);
        if (willBeCalled) {
            result.expects(once()).method("getIncludedResults").will(returnValue(included));
        }
        return (MessageResult) result.proxy();
    }
}
