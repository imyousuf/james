/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
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
import org.apache.james.fetchpop.ReaderInputStream;

/**
 * Fetch mail. Can be configured and extended to test specific pop3
 * operations.
 */
public class POP3Test extends BaseTest {
    private String host, username, password;

    public POP3Test(String name) {
        super(name);
    }

    public void configure(Configuration configuration) 
        throws ConfigurationException 
    {
        this.host = configuration.getChild("host").getValue();
        this.username = configuration.getChild("username").getValue();
        this.password = configuration.getChild("password").getValue();
        super.configure(configuration);
    }

    private POP3Client client;

    protected void setUp() throws Exception {
        client = new POP3Client();
        client.connect(host);
    }

    protected void tearDown() throws Exception {
        client.disconnect();
    }

    // ----------- pop3 operations ------------

    private POP3MessageInfo[] msg;
    private static int saveMsgCounter;

    public void login() throws Exception {
        client.login(username, password);
    }
    public void logout() throws Exception {
        client.logout();
    }
    public void fetchMsgsInfo() throws Exception {
        msg = client.listMessages();
    }

    public void fetchMsgs() throws Exception {
        for ( int i = 0 ; i < msg.length ; i++ )
            fetchMsg(msg[i],false);
    }

    public void fetchAndSaveMsgs() throws Exception {
        for ( int i = 0 ; i < msg.length ; i++ )
            fetchMsg(msg[i],true);
    }

    public void deleteMsgs() throws Exception {
        for ( int i = 0 ; i < msg.length ; i++ )
            client.deleteMessage(msg[i].number);
    }

    private void fetchMsg(POP3MessageInfo msg,boolean save) throws Exception {
        InputStream in = new ReaderInputStream(client.retrieveMessage(msg.number));
        try {
            MimeMessage message = new MimeMessage(null, in);
            if ( save ) {
                OutputStream out = new FileOutputStream
                    ("pop3test-"+host+"-"+username+"."+(saveMsgCounter++)+".eml");
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
