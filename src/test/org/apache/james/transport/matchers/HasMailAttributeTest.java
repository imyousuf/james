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

import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMatcherConfig;
import org.apache.mailet.Matcher;

import javax.mail.MessagingException;

public class HasMailAttributeTest extends AbstractHasMailAttributeTest {

    public HasMailAttributeTest() {
        super();
    }

    protected void setupMatcher() throws MessagingException {
        setupMockedMimeMessage();
        matcher = createMatcher();
        MockMatcherConfig mci = new MockMatcherConfig("HasMailAttribute="
                + getHasMailAttribute(), new MockMailContext());
        matcher.init(mci);
    }

    protected Matcher createMatcher() {
        return new HasMailAttribute();
    }

    protected String getHasMailAttribute() {
        return MAIL_ATTRIBUTE_NAME;
    }

}
