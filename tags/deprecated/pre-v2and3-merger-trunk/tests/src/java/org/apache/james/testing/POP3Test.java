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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.mail.internet.MimeMessage;

import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.fetchmail.ReaderInputStream;



/**
 * Fetch mail. Can be configured and extended to test specific POP3
 * operations.
 */
public class POP3Test extends BaseTest {

    /**
     * The POP3 host to be tested.
     */
    private String host;

    /**
     * The name of the user account being used to send the test emails.
     */
    private String username;

    /**
     * The password of the user account being used to send the test emails.
     */
    private String password;

    /**
     * Sole constructor for the test case.
     *
     * @param name the name of the test case
     */
    public POP3Test(String name) {
        super(name);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration configuration)
            throws ConfigurationException {
        this.host = configuration.getChild("host").getValue();
        this.username = configuration.getChild("username").getValue();
        this.password = configuration.getChild("password").getValue();
        super.configure(configuration);
    }

    /**
     * POP3 client that provides a simple interface to the POP3 interaction
     * from the client side.
     */
    private POP3Client client;

    /**
     * Sets up the test case by creating the POP3 client and connecting
     * to the host.
     */
    protected void setUp() throws Exception {
        client = new POP3Client();
        client.connect(host);
    }

    /**
     * Tears down the test case by disconnecting.
     */
    protected void tearDown() throws Exception {
        client.disconnect();
    }

    // ----------- pop3 operations ------------

    /**
     * The list of messages on the server.
     */
    private POP3MessageInfo[] msg;

    /**
     * The number of messages retrieved
     */
    private static int saveMsgCounter;

    /**
     * Login to the POP3 server.
     */
    public void login() throws Exception {
        client.login(username, password);
    }

    /**
     * Logout of the POP3 server.
     */
    public void logout() throws Exception {
        client.logout();
    }

    /**
     * Fetches the list of messages from the POP3 server.
     */
    public void fetchMsgsInfo() throws Exception {
        msg = client.listMessages();
    }

    /**
     * Fetches all messages from the POP3 server.
     */
    public void fetchMsgs() throws Exception {
        for (int i = 0; i < msg.length; i++) {
            fetchMsg(msg[i], false);
        }
    }

    /**
     * Fetches all messages from the POP3 server and saves them
     * to disk.
     */
    public void fetchAndSaveMsgs() throws Exception {
        for (int i = 0; i < msg.length; i++) {
            fetchMsg(msg[i], true);
        }
    }

    /**
     * Deletes all the messages from the server.
     */
    public void deleteMsgs() throws Exception {
        for (int i = 0; i < msg.length; i++) {
            client.deleteMessage(msg[i].number);
        }
    }

    /**
     * Fetches a message from the POP3 server and potentially saves it
     * to disk.
     *
     * @param msg the information for the particular message to retrieve
     * @param save whether the message is saved to a file.
     */
    private void fetchMsg(POP3MessageInfo msg, boolean save) throws Exception {
        InputStream in = new ReaderInputStream(client.retrieveMessage(msg.number));
        try {
            MimeMessage message = new MimeMessage(null, in);
            if (save) {
                OutputStream out = new FileOutputStream
                        ("pop3test-" + host + "-" + username + "." + (saveMsgCounter++) + ".eml");
                try {
                    message.writeTo(out);
                } finally {
                    IOUtil.shutdownStream(out);
                }
            }
        } finally {
            IOUtil.shutdownStream(in);
        }
    }
}
