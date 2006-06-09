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

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.mail.MessagingException;

public class RemoteAddrInNetworkTest extends AbstractRemoteAddrInNetworkTest {

    private final String ALLOWED_NETWORK = "192.168.200.0/24";

    public RemoteAddrInNetworkTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    // test if the recipients get returned as matched
    public void RemoteAddrInNetworkMatched() throws MessagingException {
        setRemoteAddr("192.168.200.1");

        setupAll();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

    // test if no recipient get returned cause it not match
    public void RemoteAddrInNetworkNotMatch() throws MessagingException {
        setRemoteAddr("192.168.1.1");

        setupAll();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }

    protected AbstractNetworkMatcher createMatcher() {
        return new RemoteAddrInNetwork();
    }

    protected String getConfigOption() {
        return "AllowedNetworkIs=";
    }

    protected String getAllowedNetworks() {
        return ALLOWED_NETWORK;
    }
}
