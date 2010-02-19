/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.camel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;

/**
 * A Splitter for use with Camel to split the MailMessage into many pieces if needed. This is done
 * by use a Matcher.
 * 
 *
 */
public class MatcherSplitter {
    
    public final static String MATCHER_MATCHED_HEADER = "matched";
    
    public final static String MATCHER_HEADER = "matcher";

    /**
     * Generate a List of MailMessage instances for the give @Body. This is done by using the given Matcher to see if we need more then one instance of the 
     * MailMessage
     * 
     * @param matcher Matcher to use for splitting
     * @param mail Mail which is stored in the @Body of the MailMessage
     * @return mailMessageList
     * @throws MessagingException
     */
    public List<MailMessage> split(@Header(MATCHER_HEADER) Matcher matcher,@Body Mail mail) throws MessagingException {
        //System.out.println("Call matcher " + matcher);
        List<MailMessage> mails = new ArrayList<MailMessage>();
        boolean fullMatch = false;
        
        
        Collection<MailAddress> matchedRcpts = matcher.match(mail);
        
        // check if the matcher matched
        if (matchedRcpts != null &&  matchedRcpts.isEmpty() == false) {
            
            // check if we need to create another instance of the mail. This is only needed if not all
            // recipients matched 
            if (matchedRcpts.equals(mail.getRecipients()) == false) {
                Mail newMail = new MailImpl(mail);
                newMail.setRecipients(matchedRcpts);
            
                MailMessage newmsg = new MailMessage(newMail);
                
                // Set a header because the matcher matched. This can be used later when processing the route
                newmsg.setHeader(MATCHER_MATCHED_HEADER, true);
                mails.add(newmsg);
            
                List<MailAddress> rcpts = new ArrayList<MailAddress>(mail.getRecipients());
                Iterator<MailAddress> rcptsIterator = newMail.getRecipients().iterator();
                while(rcptsIterator.hasNext()) {
                    rcpts.remove(rcptsIterator.next());
                }
                mail.setRecipients(rcpts);
            } else {
                // all recipients matched
                fullMatch = true;
            }
        }
        MailMessage mailMsg = new MailMessage(mail);
        if (fullMatch) {
            // Set a header because the matcher matched. This can be used later when processing the route
            mailMsg.setHeader("match", true);
        }
        mails.add(mailMsg);
        
        return mails;
    }
}
