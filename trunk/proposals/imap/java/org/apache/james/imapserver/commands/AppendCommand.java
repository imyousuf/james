/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.AccessControlException;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.FileMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.Flags;
import org.apache.james.AuthorizationException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Date;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import com.sun.mail.iap.Literal;

/**
 * Appends new Mails to the IMAP Store. 
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @version 0.3 on 08 Aug 2002
 */
class AppendCommand extends AuthenticatedSelectedStateCommand {
    public AppendCommand(){
        System.out.println("APPEND STARTED");
        this.commandName = "APPEND";
        this.getArgs().add( new AstringArgument( "mailbox" ) );
        this.getArgs().add( new AstringArgument( "flags" ) );
        this.getArgs().add( new AstringArgument( "date" ) );
        this.getArgs().add( new AstringArgument( "message" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues ) {
        String command = this.getCommand();
        ByteArrayOutputStream byteout = new ByteArrayOutputStream();
        java.io.PrintWriter out = session.getOut();
        BufferedReader bre = session.getIn();
        
        try{
            String ms = (String) argValues.get(3);
            long messagelen = Long.parseLong(ms.substring(1,ms.length()-1));
            long messageleft = messagelen;
           
            session.setCanParseCommand(false);
            out.println("+ go ahead");

            while(messageleft > 0) {
                int buffer = 200;
                if (messageleft<200) buffer = (int) messageleft;
                char[] cbuf = new char[buffer] ;
                bre.read(cbuf);
                mailb.append(String.copyValueOf(cbuf));
                byteout.write(String.valueOf(cbuf).getBytes());
                messageleft = messageleft - buffer;
            }
            session.setCanParseCommand(true);
        }catch(Exception e){}
        
        String folder = (String) argValues.get( 0 );
        FileMailbox mailbox = (FileMailbox)getMailbox( session, folder, command );
        
        if ( mailbox == null ) {
            session.noResponse( command, "Must specify a mailbox." );
            return true;
        } else {
            session.setCurrentMailbox( mailbox );
        }
        try { // long tries clause against an AccessControlException
            if ( !session.getCurrentMailbox().hasInsertRights( session.getCurrentUser() ) ) {
                session.noResponse( command, "Insert access not granted." );
                return true;
            }
            
            //This must be the flags
            String flags = (String) argValues.get(1);
            //This must be the internalDate string
            String date = (String) argValues.get(2);
            
            MimeMessageInputStreamSource source = new MimeMessageInputStreamSource("Mail" + System.currentTimeMillis() + "-" + mailbox.getNextUID(), new ByteArrayInputStream(byteout.toByteArray()));
            MimeMessageWrapper msg = new MimeMessageWrapper(source);

            try{
                msg.setHeader("Received", date);
                msg.saveChanges();
            }catch (MessagingException me){
                    //ignore
            }
            mailbox.store( (MimeMessage)msg, session.getCurrentUser() );
            
            session.okResponse( "append completed" );
            return true;
        } catch ( AuthorizationException aze ) {
            System.out.println("AUTHERROR");
            session.noResponse( command, "append error: can't append to that mailbox, error in flags or date/time or message text." );
            session.logAZE( aze );
            return true;
        } catch ( AccessControlException ace ) {
            System.out.println("ACCESSERROR");
            session.noResponse( command, "[TRYCREATE " + folder + "] No such mailbox." );
            session.logACE( ace );
            return true;
        }
    }
}

