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

package org.apache.james.transport.matchers;

import org.apache.mailet.Matcher;

import javax.mail.MessagingException;
import java.util.Collection;

public class HasMailAttributeWithValueRegexTest extends AbstractHasMailAttributeTest {

    private String regex = ".*";

    public HasMailAttributeWithValueRegexTest() {
        super();
    }

    private void setRegex(String regex) {
        this.regex = regex;
    }

    protected String getHasMailAttribute() {
        return MAIL_ATTRIBUTE_NAME + ", " + regex;
    }

    protected Matcher createMatcher() {
        return new HasMailAttributeWithValueRegex();
    }

// test if the mail attribute was matched
    public void testAttributeIsMatched() throws MessagingException {
        init();
        setRegex(".*");
        setupAll();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

    // test if the mail attribute was not matched
    public void testHeaderIsNotMatched() throws MessagingException {
        setRegex("\\d");
        setupAll();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }

    // test if an exception was thrown cause the regex was invalid
    public void testHeaderIsNotMatchedCauseValue() throws MessagingException {

        String invalidRegex = "(!(";
        String regexException = null;
        String exception = "Malformed pattern: " + invalidRegex;

        setRegex(invalidRegex);
        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);

        try {
            setupMatcher();
        } catch (MessagingException m) {
            regexException = m.getMessage();
        }

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
        assertEquals(regexException, exception);

    }
}
