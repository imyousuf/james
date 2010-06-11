/***********************************************************************
 * Copyright (c) 2003-2006 The Apache Software Foundation.             *
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

package org.apache.james.transport.matchers;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.services.JamesUser;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

import javax.mail.MessagingException;

import java.util.Iterator;

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

    private MailServer mailServer;

    /** The store containing the local user repository. */
    private UsersStore usersStore;

    /** The user repository for this mail server.  Contains all the users with inboxes
     * on this server.
     */
    private UsersRepository localusers;

    /**
     * Standard matcher initialization.
     * Overriding classes must do a <CODE>super.init()</CODE>.
     */
    public void init() throws MessagingException {
        super.init();
        ServiceManager compMgr = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            mailServer = (MailServer) compMgr.lookup(MailServer.ROLE);
        } catch (ServiceException e) {
            log("Exception in getting the MailServer: " + e.getMessage() + e.getKey());
        }        
        try {
            usersStore = (UsersStore)compMgr.lookup(UsersStore.ROLE);
        } catch (ServiceException e) {
            log("Exception in getting the UsersStore: " + e.getMessage() + e.getKey());
        }        
        localusers = (UsersRepository)usersStore.getRepository("LocalUsers");
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
        MailRepository userInbox = mailServer.getUserInbox(getPrimaryName(recipient.getUser()));
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
        }
        return size;
    }

    /**
     * Gets the main name of a local customer, handling aliases.
     *
     * @param originalUsername the user name to look for; it can be already the primary name or an alias
     * @return the primary name, or originalUsername unchanged if not found
     */
    protected String getPrimaryName(String originalUsername) {
        String username;
        try {
            username = localusers.getRealName(originalUsername);
            JamesUser user = (JamesUser) localusers.getUserByName(username);
            if (user.getAliasing()) {
                username = user.getAlias();
            }
        }
        catch (Exception e) {
            username = originalUsername;
        }
        return username;
    }
    
}
