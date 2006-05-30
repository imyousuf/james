/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport.matchers;

import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMatcherConfig;

import org.apache.mailet.Matcher;

import javax.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import junit.framework.TestCase;

public class AllTest extends TestCase {

    private MockMail mockedMail;

    private Matcher matcher;

    public AllTest(String arg0) throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setupMockedMail() {
        mockedMail = new MockMail();
        mockedMail.setRecipients(Arrays.asList(new String[] {
                "test@james.apache.org", "test2@james.apache.org" }));

    }

    private void setupMatcher() throws MessagingException {
        matcher = new All();
        MockMatcherConfig mci = new MockMatcherConfig("All",
                new MockMailContext());
        matcher.init(mci);
    }

    // test if all recipients was returned
    public void testAllRecipientsReturned() throws MessagingException {
        setupMockedMail();
        setupMatcher();

        assertTrue(matcher.match(mockedMail) != null);
        assertEquals(matcher.match(mockedMail).size(), mockedMail
                .getRecipients().size());
    }

}
