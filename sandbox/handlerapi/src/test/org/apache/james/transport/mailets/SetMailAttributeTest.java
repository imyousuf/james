/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
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

package org.apache.james.transport.mailets;

import junit.framework.TestCase;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.james.test.util.Util;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public class SetMailAttributeTest extends TestCase {

    private Mail mockedMail;

    private Mailet mailet;

    private final String ATTRIBUTE_NAME1 = "org.apache.james.junit1";

    private final String ATTRIBUTE_NAME2 = "org.apache.james.junit2";

    public SetMailAttributeTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setupMailet() throws MessagingException {
        mailet = new SetMailAttribute();
        MockMailetConfig mci = new MockMailetConfig("Test",
                new MockMailContext());
        mci.setProperty(ATTRIBUTE_NAME1, "true");
        mci.setProperty(ATTRIBUTE_NAME2, "true");

        mailet.init(mci);
    }

    // test if the Header was add
    public void testMailAttributeAdded() throws MessagingException {
        mockedMail = Util.createMockMail2Recipients(null);
        setupMailet();

        assertNull(mockedMail.getAttribute(ATTRIBUTE_NAME1));
        assertNull(mockedMail.getAttribute(ATTRIBUTE_NAME2));
        mailet.service(mockedMail);

        assertEquals("true", mockedMail.getAttribute(ATTRIBUTE_NAME1));
        assertEquals("true", mockedMail.getAttribute(ATTRIBUTE_NAME2));
    }
}
