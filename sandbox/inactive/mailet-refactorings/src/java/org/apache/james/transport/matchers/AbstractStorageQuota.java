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



package org.apache.james.transport.matchers;

import java.util.Iterator;
import javax.mail.MessagingException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailRepository;
import org.apache.mailet.MailetContext;

/**
 * <P>Abstract matcher checking whether a recipient has exceeded a maximum allowed
 * <I>storage</I> quota for messages standing in his inbox.</P>
 * <P>"Storage quota" at this level is still an abstraction whose specific interpretation
 * will be done by subclasses (e.g. could be specific for each user or common to all of them).</P> 
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
abstract public class AbstractStorageQuota extends AbstractQuotaMatcher { 

    


    /**
     * Standard matcher initialization.
     * Overriding classes must do a <CODE>super.init()</CODE>.
     */
    public void init() throws MessagingException {
        super.init();
        

    }

    /** 
     * Checks the recipient.
     * Does a <CODE>super.isRecipientChecked</CODE> and checks that the recipient
     * is a known user in the local server.
     * If a subclass overrides this method it should "and" <CODE>super.isRecipientChecked</CODE>
     * to its check.
     *
     * @param recipient the recipient to check
     */    
    protected boolean isRecipientChecked(MailAddress recipient) throws MessagingException {
        MailetContext mailetContext = getMailetContext();
        return super.isRecipientChecked(recipient) && (mailetContext.isLocalEmail(recipient));
    }

    /** 
     * Gets the storage used in the recipient's inbox.
     *
     * @param recipient the recipient to check
     */    
    protected long getUsed(MailAddress recipient) throws MessagingException {
        long size = 0;
        MailRepository userInbox = getMailetContext().getMailRepository(recipient);
        for (Iterator it = userInbox.list(); it.hasNext(); ) {
            String key = (String) it.next();
            Mail mc = userInbox.retrieve(key);
            // Retrieve can return null if the mail is no longer in the store.
            if (mc != null) try {
                size += mc.getMessageSize();
            } catch (Throwable e) {
                // MailRepository.retrieve() does NOT lock the message.
                // It could be deleted while we're looping.
                log("Exception in getting message size: " + e.getMessage());
            }
            ContainerUtil.dispose(mc);
        }
        return size;
    }

    
    
    
}
