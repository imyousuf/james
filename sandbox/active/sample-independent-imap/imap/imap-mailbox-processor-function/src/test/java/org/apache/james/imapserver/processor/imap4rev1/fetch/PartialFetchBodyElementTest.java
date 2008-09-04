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

package org.apache.james.imapserver.processor.imap4rev1.fetch;

import org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class PartialFetchBodyElementTest extends MockObjectTestCase {

    private static final long NUMBER_OF_OCTETS = 100;
    
    Mock mockBodyElement;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockBodyElement = mock(BodyElement.class);
        mockBodyElement.expects(once()).method("getName").will(returnValue("Name"));
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSizeShouldBeNumberOfOctetsWhenSizeMoreWhenStartIsZero() throws Exception {
        long moreThanNumberOfOctets = NUMBER_OF_OCTETS + 1;
        PartialFetchBodyElement element = new PartialFetchBodyElement((BodyElement) mockBodyElement.proxy(), 
                0, NUMBER_OF_OCTETS);
        mockBodyElement.expects(once()).method("size").will(returnValue(new Long(moreThanNumberOfOctets)));
        assertEquals("Size is more than number of octets so should be number of octets", NUMBER_OF_OCTETS, element.size());
    }
    
    public void testSizeShouldBeSizeWhenNumberOfOctetsMoreWhenStartIsZero() throws Exception {
        long lessThanNumberOfOctets = NUMBER_OF_OCTETS -1;
        PartialFetchBodyElement element = new PartialFetchBodyElement((BodyElement) mockBodyElement.proxy(), 
                0, NUMBER_OF_OCTETS);
        mockBodyElement.expects(once()).method("size").will(returnValue(new Long(lessThanNumberOfOctets)));
        assertEquals("Size is less than number of octets so should be size", lessThanNumberOfOctets, element.size());
    }
    
    public void testWhenStartPlusNumberOfOctetsIsMoreThanSizeSizeShouldBeSizeMinusStart() throws Exception {
        long size = 60;
        PartialFetchBodyElement element = new PartialFetchBodyElement((BodyElement) mockBodyElement.proxy(), 
                10, NUMBER_OF_OCTETS);
        mockBodyElement.expects(once()).method("size").will(returnValue(new Long(size)));
        assertEquals("Size is less than number of octets so should be size", 50, element.size());
    }
    
    public void testWhenStartPlusNumberOfOctetsIsLessThanSizeSizeShouldBeNumberOfOctetsMinusStart() throws Exception {
        long size = 100;
        PartialFetchBodyElement element = new PartialFetchBodyElement((BodyElement) mockBodyElement.proxy(), 
                10, NUMBER_OF_OCTETS);
        mockBodyElement.expects(once()).method("size").will(returnValue(new Long(size)));
        assertEquals("Size is less than number of octets so should be size", 90, element.size());
    }
    
    public void testSizeShouldBeZeroWhenStartIsMoreThanSize() throws Exception {
        long size = 100;
        PartialFetchBodyElement element = new PartialFetchBodyElement((BodyElement) mockBodyElement.proxy(), 
                1000, NUMBER_OF_OCTETS);
        mockBodyElement.expects(once()).method("size").will(returnValue(new Long(size)));
        assertEquals("Size is less than number of octets so should be size", 0, element.size());
    }
    
    public void testSizeShouldBeNumberOfOctetsWhenStartMoreThanOctets() throws Exception {
        long size = 2000;
        PartialFetchBodyElement element = new PartialFetchBodyElement((BodyElement) mockBodyElement.proxy(), 
                1000, NUMBER_OF_OCTETS);
        mockBodyElement.expects(once()).method("size").will(returnValue(new Long(size)));
        assertEquals("Content size is less than start. Size should be zero.", NUMBER_OF_OCTETS, element.size());
    }
}
