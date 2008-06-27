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

package org.apache.james.imapserver.commands;

import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.FileMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.Flags;
import org.apache.james.imapserver.AuthorizationException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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
    
    public boolean process( ImapRequest request, ImapSession session ) {
        StringTokenizer tokens = request.getCommandLine();
        List argValues = new ArrayList();

        boolean fetchNextTogether = false;
        String tokensave = "";
        while(tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            System.out.println("FOUND TOKEN: "+token);
            if (token.startsWith("(") || token.startsWith("\"")) {
                // Fetch token
                token = token.substring(1); 
                if (token.endsWith(")") || token.endsWith("\"")) {
                    token = token.substring(0,token.length()-1);
                    argValues.add(token);
                    System.out.println("ADDED1 TOKEN: "+token);
                }else{
                    fetchNextTogether = true;
                    tokensave = tokensave+token;
                }
            }else if(token.endsWith(")") || token.endsWith("\"")) {
                token = token.substring(0,token.length()-1);
                argValues.add(tokensave+" "+token);
                System.out.println("ADDED2 TOKEN: "+tokensave+" "+token);
                tokensave = "";
                fetchNextTogether = false;
            }else if(fetchNextTogether) {
                tokensave = tokensave+" "+token;
            }else{
                argValues.add(token);
                System.out.println("ADDED3 TOKEN: "+token);
            }   
        }
        return doProcess( request, session, argValues );
    }
    
    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues ) {
        String command = this.getCommand();
        ByteArrayOutputStream byteout = new ByteArrayOutputStream();
        java.io.PrintWriter out = session.getOut();
        BufferedReader bre = session.getIn();
        
        String folder = (String) argValues.get( 0 );
        String ms = "";
        String flags = "";
        String date = "";
        
        String arg1 = "";
        String arg2 = "";
        String arg3 = "";
        try {
            arg1 = (String) argValues.get(1);
            arg2 = (String) argValues.get(2);
            arg3 = (String) argValues.get(3);
        }catch(Exception e) {}
        
        if (arg1.startsWith("{")) {
            // No Argues
            ms = arg1;
        }else if (arg1.startsWith("\\")) {
            // arg1 = flags
            flags = arg1;
            if (arg3.startsWith("{")) {
                date = arg2;
                ms = arg3;
            }else{
                ms = arg2;
            }
        }else if (arg2.startsWith("\\")) {
            // arg2 = flags
            flags = arg2;
            ms = arg3;
        }
        System.out.println("APPEND: ms is "+ms);
        System.out.println("APPEND: flags is "+flags);
        System.out.println("APPEND: date is "+date);
        
        try{    
            long messagelen = Long.parseLong(ms.substring(1,ms.length()-2));
            long messageleft = messagelen;
           
            session.setCanParseCommand(false);
            out.println("+ go ahead");

            while(messageleft > 0) {
                int buffer = 200;
                if (messageleft<200) buffer = (int) messageleft;
                char[] cbuf = new char[buffer] ;
                bre.read(cbuf);
                byteout.write(String.valueOf(cbuf).getBytes());
                messageleft = messageleft - buffer;
            }
            session.setCanParseCommand(true);
        }catch(Exception e){
            System.out.println("Error occured parsing input");
            e.printStackTrace();
        }
        
        
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
            
            MimeMessageInputStreamSource source;
            try {
                source =
                    new MimeMessageInputStreamSource("Mail" + System.currentTimeMillis() + "-" + mailbox.getNextUID(), 
                                                     new ByteArrayInputStream(byteout.toByteArray()));
            } catch (MessagingException me) {
                me.printStackTrace();
                return false;
            }
            MimeMessageWrapper msg = new MimeMessageWrapper(source);

            try{
                msg.setHeader("Received", date);
                msg.saveChanges();
            }catch (MessagingException me){
                me.printStackTrace();
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

