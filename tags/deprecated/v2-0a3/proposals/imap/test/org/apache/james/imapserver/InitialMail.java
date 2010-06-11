/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import junit.framework.TestCase;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import java.util.Properties;

public final class InitialMail extends TestCase
        implements IMAPTest
{
    private Session _session;
    private InternetAddress _fromAddress;
    private InternetAddress _toAddress;

    public InitialMail( String name )
    {
        super( name );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        Properties props = new Properties();
        _session = Session.getDefaultInstance( props );

        _fromAddress = new InternetAddress( FROM_ADDRESS );
        _toAddress = new InternetAddress( TO_ADDRESS );
    }

    public void testSendInitialMessages() throws Exception
    {
        sendMessage( "Message 1", "This is the first message." );
        sendMessage( "Message 2", "This is the second message." );
        sendMessage( "Message 3", "This is the third message." );
        sendMessage( "Message 4", "This is the fourth message." );
    }

    private void sendMessage( String subject, String body )
            throws Exception
    {
        MimeMessage msg = new MimeMessage(_session);
        msg.setFrom( _fromAddress );
        msg.addRecipient(Message.RecipientType.TO, _toAddress );
        msg.setSubject( subject );
        msg.setContent( body, "text/plain" );

        Transport.send( msg );
        System.out.println( "Sending message: " + subject );
        
        Thread.sleep( 1000 );
    }
}
