/**
 * DeliveryTests.java
 *
 * Copyright (C) 06-Jan-2003 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;
import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.smtp.SMTPClient;

import junit.framework.TestCase;
/** * * $Id: DeliveryTests.java,v 1.2 2003/01/27 02:15:31 serge Exp $ */
public class DeliveryTests extends TestCase {    /**     * Constructor for DeliveryTests.     * @param arg0     */

    public DeliveryTests(String arg0) {        super(arg0);    }

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
