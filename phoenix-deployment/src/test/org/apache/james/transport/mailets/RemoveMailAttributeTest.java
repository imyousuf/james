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

package org.apache.james.transport.mailets;

import junit.framework.TestCase;

import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

public class RemoveMailAttributeTest extends TestCase {

    public static final String MAIL_ATTRIBUTE_NAME1 = "org.apache.james.test.junit";

    public static final String MAIL_ATTRIBUTE_NAME2 = "org.apache.james.test.junit2";

    private Mail setupMockedMail() throws ParseException {
        Mail mockedMail = new MockMail();
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME1, "true");
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME2, "true");
        return mockedMail;
    }

    private Mailet setupMailet(String attribute) throws MessagingException {
        Mailet mailet = new RemoveMailAttribute();
        MockMailetConfig mci = new MockMailetConfig("Test",
                new MockMailContext());
        if (attribute != null) {
            mci.setProperty("name", attribute);
        }

        mailet.init(mci);
        return mailet;
    }


    public void testRemoveMailAttribute() throws MessagingException {
        Mail m = setupMockedMail();
        Mailet mailet = setupMailet(MAIL_ATTRIBUTE_NAME1);

        // check if the mail has a attribute
        assertNotNull("Attribute exists",m.getAttribute(MAIL_ATTRIBUTE_NAME1));
        assertNotNull("Attribute exists",m.getAttribute(MAIL_ATTRIBUTE_NAME2));

        mailet.service(m);

        // Check if the attribute was removed
        assertNull("Attribute exists",m.getAttribute(MAIL_ATTRIBUTE_NAME1));
        assertNotNull("Attribute deleted",m.getAttribute(MAIL_ATTRIBUTE_NAME2));
    }


    public void testInvalidConfig() throws MessagingException {
        boolean exception = false;
        try {
            setupMailet(null);
        } catch (MessagingException e) {
            exception = true;
        }

        assertTrue("invalid Config", exception);
    }

}
