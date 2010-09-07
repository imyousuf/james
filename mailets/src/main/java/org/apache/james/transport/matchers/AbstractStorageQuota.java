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

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.james.api.user.JamesUser;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.util.FetchGroupImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

/**
 * <P>Experimental: Abstract matcher checking whether a recipient has exceeded a maximum allowed
 * <I>storage</I> quota for messages standing in his inbox.</P>
 * <P>"Storage quota" at this level is still an abstraction whose specific interpretation
 * will be done by subclasses (e.g. could be specific for each user or common to all of them).</P> 
 *
 * <P>This matcher need to calculate the mailbox size everytime it is called. This can slow down things if there are many mails in
 * the mailbox. Some users also report big problems with the matcher if a JDBC based mailrepository is used. </P>
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
abstract public class AbstractStorageQuota extends AbstractQuotaMatcher { 

    private MailboxManager manager;

    @Resource(name="mailboxmanager")
    public void setMailboxManager(MailboxManager manager) {
        this.manager = manager;
    }
    
    @Resource(name="localusersrepository")
    public void setUsersRepository(UsersRepository localusers) {
        this.localusers = localusers;
    }
    /** The user repository for this mail server.  Contains all the users with inboxes
     * on this server.
     */
    private UsersRepository localusers;

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
    protected long getUsed(MailAddress recipient, Mail _) throws MessagingException {
        long size = 0;
        MailboxSession session = manager.createSystemSession(getPrimaryName(recipient.getLocalPart()), new Log() {

			public void debug(Object arg0) {
				// just consume 				
			}

			public void debug(Object arg0, Throwable arg1) {
				// just consume 
			}

			public void error(Object arg0) {
				log(arg0.toString());

			}

			public void error(Object arg0, Throwable arg1) {
				log(arg0.toString(),arg1);
			}

			public void fatal(Object arg0) {
				log(arg0.toString());
			}

			public void fatal(Object arg0, Throwable arg1) {
				log(arg0.toString(), arg1);				
			}

			public void info(Object arg0) {
				log(arg0.toString());
			}

			public void info(Object arg0, Throwable arg1) {
				log(arg0.toString(), arg1);
				
			}

			public boolean isDebugEnabled() {
				return false;
			}

			public boolean isErrorEnabled() {
				return true;
			}

			public boolean isFatalEnabled() {
				return true;
			}

			public boolean isInfoEnabled() {
				return true;
			}

			public boolean isTraceEnabled() {
				return false;
			}

			public boolean isWarnEnabled() {
				return true;
			}

			public void trace(Object arg0) {
				// just consume 				
			}

			public void trace(Object arg0, Throwable arg1) {
				// just consume 				
			}

			public void warn(Object arg0) {
				log(arg0.toString());
			}

			public void warn(Object arg0, Throwable arg1) {
				log(arg0.toString(), arg1);
			}
        	
        });
        manager.startProcessingRequest(session);
        MessageManager mailbox = manager.getMailbox(new MailboxPath(MailboxConstants.USER_NAMESPACE,
                                            session.getUser().getUserName(), "INBOX"),
                                            session);
        Iterator<MessageResult> results = mailbox.getMessages(MessageRange.all(), new FetchGroupImpl(FetchGroup.MINIMAL), session);
        
        while (results.hasNext()) {
        	size += results.next().getSize();
        }
        manager.endProcessingRequest(session);
        manager.logout(session, true);
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
