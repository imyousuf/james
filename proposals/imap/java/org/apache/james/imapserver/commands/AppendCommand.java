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

