/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.testing;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.mail.internet.MimeMessage;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.net.SocketClient;
import org.apache.commons.net.pop3.POP3;
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
