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
import java.util.Collection;
import java.util.ArrayList;
import javax.mail.MessagingException;
import org.apache.mailet.GenericMatcher;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mail;

/**
 * <P>Abstract matcher checking whether a recipient has exceeded a maximum allowed quota.</P>
 * <P>"Quota" at this level is an abstraction whose specific interpretation
 * will be done by subclasses.</P> 
 * <P>Although extending GenericMatcher, its logic is recipient oriented.</P>
 *
 * @version 1.0.0, 2003-05-11
 */
abstract public class AbstractQuotaMatcher extends GenericMatcher { 

    /**
     * Standard matcher entrypoint.
     * First of all, checks the sender using {@link #isSenderChecked}.
     * Then, for each recipient checks it using {@link #isRecipientChecked} and
     * {@link #isOverQuota}.
     *
     * @throws MessagingException if either <CODE>isSenderChecked</CODE> or isRecipientChecked throw an exception
     */    
    public final Collection match(Mail mail) throws MessagingException {
        Collection matching = null;
        if (isSenderChecked(mail.getSender())) {
            matching = new ArrayList();
            for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
                MailAddress recipient = (MailAddress) i.next();
                if (isRecipientChecked(recipient) && isOverQuota(recipient, mail)) {
                    matching.add(recipient);
                }
            }
        }
        return matching;
    }

    /**
     * Does the quota check.
     * Checks if {@link #getQuota} < {@link #getUsed} for a recipient.
     * Catches any throwable returning false, and so should any override do.
     *
     * @param address the recipient addresss to check
     * @param mail the mail involved in the check
     * @return true if over quota
     */
    protected boolean isOverQuota(MailAddress address, Mail mail) {
        String user = address.getUser();
        try {
            boolean over = getQuota(address, mail) < getUsed(address, mail);
            if (over) log(address + " is over quota.");
            return over;
        } catch (Throwable e) {
            log("Exception checking quota for: " + address, e);
            return false;
        }
    }

    /** 
     * Checks the sender.
     * The default behaviour is to check that the sender <I>is not</I> the local postmaster.
     * If a subclass overrides this method it should "and" <CODE>super.isSenderChecked</CODE>
     * to its check.
     *
     * @param sender the sender to check
     */    
    protected boolean isSenderChecked(MailAddress sender) throws MessagingException {
        return !(getMailetContext().getPostmaster().equals(sender));
    }

    /** 
     * Checks the recipient.
     * The default behaviour is to check that the recipient <I>is not</I> the local postmaster.
     * If a subclass overrides this method it should "and" <CODE>super.isRecipientChecked</CODE>
     * to its check.
     *
     * @param recipient the recipient to check
     */    
    protected boolean isRecipientChecked(MailAddress recipient) throws MessagingException {
        return !(getMailetContext().getPostmaster().equals(recipient));
    }

    /** 
     * Gets the quota to check against.
     *
     * @param address the address holding the quota if applicable
     * @param mail the mail involved if needed
     */    
    abstract protected long getQuota(MailAddress address, Mail mail) throws MessagingException;
    
    /**
     * Gets the used amount to check against the quota.
     *
     * @param address the address involved
     * @param mail the mail involved if needed
     */
    abstract protected long getUsed(MailAddress address, Mail mail) throws MessagingException;

    /**
     * Utility method that parses an amount string.
     * You can use 'k' and 'm' as optional postfixes to the amount (both upper and lowercase).
     * In other words, "1m" is the same as writing "1024k", which is the same as
     * "1048576".
     *
     * @param amount the amount string to parse
     */
    protected long parseQuota(String amount) throws MessagingException {
        long quota;
        try {
            if (amount.endsWith("k")) {
                amount = amount.substring(0, amount.length() - 1);
                quota = Long.parseLong(amount) * 1024;
            } else if (amount.endsWith("m")) {
                amount = amount.substring(0, amount.length() - 1);
                quota = Long.parseLong(amount) * 1024 * 1024;
            } else {
                quota = Long.parseLong(amount);
            }
            return quota;
        }
        catch (Exception e) {
            throw new MessagingException("Exception parsing quota", e);
        }
    }
}
