/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.james.transport.matchers;

import java.util.Iterator;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;

import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailServer;
import org.apache.james.services.MailRepository;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

import javax.mail.MessagingException;

/**
 * <P>Abstract matcher checking whether a recipient has exceeded a maximum allowed
 * <I>storage</I> quota for messages standing in his inbox.</P>
 * <P>"Storage quota" at this level is still an abstraction whose specific interpretation
 * will be done by subclasses (e.g. could be specific for each user or common to all of them).</P> 
 *
 * @version 1.0.0, 2003-05-11
 */
abstract public class AbstractStorageQuota extends AbstractQuotaMatcher { 

    private MailServer mailServer;

    /**
     * Standard matcher initialization.
     * Overriding classes must do a <CODE>super.init()</CODE>.
     */
    public void init() throws MessagingException {
        super.init();
        ComponentManager compMgr = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            mailServer = (MailServer) compMgr.lookup(MailServer.ROLE);
        } catch (ComponentException e) {
            log("Exception in getting the MailServer: " + e.getMessage() + e.getRole());
        }        
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
        return super.isRecipientChecked(recipient) && (mailetContext.isLocalServer(recipient.getHost()) && mailetContext.isLocalUser(recipient.getUser()));
    }

    /** 
     * Gets the storage used in the recipient's inbox.
     *
     * @param recipient the recipient to check
     */    
    protected long getUsed(MailAddress recipient, Mail _) throws MessagingException {
        long size = 0;
        MailRepository userInbox = mailServer.getUserInbox(recipient.getUser());
        for (Iterator it = userInbox.list(); it.hasNext(); ) {
            String key = (String) it.next();
            MailImpl mc = userInbox.retrieve(key);
            // Retrieve can return null if the mail is no longer in the store.
            if (mc != null) try {
                size += mc.getMessageSize();
            } catch (Throwable e) {
                // MailRepository.retrieve() does NOT lock the message.
                // It could be deleted while we're looping.
                log("Exception in getting message size: " + e.getMessage());
            }
        }
        return size;
    }
}
