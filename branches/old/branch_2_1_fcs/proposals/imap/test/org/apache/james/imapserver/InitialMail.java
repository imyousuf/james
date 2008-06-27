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
        props.setProperty("mail.debug","true");
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
