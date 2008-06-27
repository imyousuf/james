/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
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

package org.apache.james.testing;


import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.smtp.SMTPClient;

import junit.framework.TestCase;


/**
 *
 * $Id: DeliveryTests.java,v 1.5 2004/01/30 02:22:20 noel Exp $
 */

public class DeliveryTests extends TestCase {

    /**
     * Constructor for DeliveryTests.
     * @param arg0
     */

    public DeliveryTests(String arg0) {
        super(arg0);
    }

    public void testSpam() {
        String spamMail = "Subject: spam test\nFrom: postmaster@localhost\nTo: noone@localhost\n\nTHIS IS A TEST";
        SMTPClient client = new SMTPClient();

        try {
            client.connect("127.0.0.1", 25);
            client.sendSimpleMessage("postmaster@localhost", "noone@localhost", spamMail);
            client.disconnect();
        } catch (SocketException e) {
            assertTrue(false);
        } catch (IOException e) {
            assertTrue(false);
        }
        assertTrue(true);
    }

    public void testOutgoing() {
        String outgoingMail = "Subject: outgoing test\nFrom: postmaster@localhost\nTo: noone@mailet.org\n\nTHIS IS A TEST";
        SMTPClient client = new SMTPClient();

        try {
            client.connect("127.0.0.1", 25);
            client.sendSimpleMessage("postmaster@localhost", "noone@mailet.org", outgoingMail);

            client.disconnect();
        } catch (SocketException e) {
            assertTrue(false);
        } catch (IOException e) {
            assertTrue(false);
        }
        assertTrue(true);
    }

}

