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

package org.apache.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;

/**
 * An abstract implementation of a listserv manager.  This mailet reads the
 * address to find the command.
 */
public abstract class GenericListservManager extends GenericMailet {

    /**
     * Adds an address to the listserv.  Returns whether command was
     * successful.
     */
    public abstract boolean addAddress(MailAddress address);

    /**
     * Removes an address from the listserv.  Returns whether command
     * was successful.
     */
    public abstract boolean removeAddress(MailAddress address);


    /**
     * Indicates whether an address already exists on the listserv. Returns
     * whether the address exists.
     */
    public abstract boolean existsAddress(MailAddress address);

    /**
     * Processes the message.  Checks which command was sent based on the
     * recipient address, and does the appropriate action.
     */
    public final void service(Mail mail) throws MessagingException {
        if (mail.getRecipients().size() != 1) {
            getMailetContext().bounce(mail, "You can only send one command at a time to this listserv manager.");
            return;
        }
        MailAddress address = (MailAddress)mail.getRecipients().iterator().next();
        if (address.getUser().endsWith("-off")) {
            if (existsAddress(mail.getSender())) {
                if (removeAddress(mail.getSender())) {
                    getMailetContext().bounce(mail, "Successfully removed from listserv.");
                } else {
                    getMailetContext().bounce(mail, "You are not subscribed to this listserv.");
                }
            } else {
                getMailetContext().bounce(mail, "Unable to remove you from listserv for some reason");
            }
        } else if (address.getUser().endsWith("-on")) {
            if (existsAddress(mail.getSender())) {
                getMailetContext().bounce(mail, "You are already subscribed to this listserv.");
            } else {
                if (addAddress(mail.getSender())) {
                    getMailetContext().bounce(mail, "Successfully added to listserv.");
                } else {
                    getMailetContext().bounce(mail, "Unable to add you to the listserv for some reason");
                }
            }
        } else {
            getMailetContext().bounce(mail, "Could not understand the command you sent to this listserv manager.\r\n"
                                      + "Valid commands are <listserv>-on@domain.com and <listserv>-off@domain.com");
        }
        //Kill the command message
        mail.setState(Mail.GHOST);
    }
}
