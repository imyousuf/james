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

package org.apache.james.transport.mailets;

import org.apache.james.transport.matchers.HasHabeasWarrantMark;
import org.apache.mailet.GenericMailet ;
import org.apache.mailet.Mail ;

/*
 * This matcher adds the Hebeas Warrant Mark to a message.
 * For details see: http://www.hebeas.com
 *
 * Usage: &lt;mailet match="&lt;suitable-matcher&gt;" class="AddHabeasWarrantMark" /&gt;
 *
 * NOTE: Although this mailet is covered by the Apache Software License,
 * the Habeas Warrant Mark is copyright.  A separate license from Habeas
 * is required in order to legally attach the Habeas Warrant Mark to
 * e-mail messages.  Each James Administrator is responsible for
 * ensuring that James is configured to attach the Habeas Warrant Mark
 * only to e-mail covered by a suitable license received from Habeas.
 *
 * Because the Habeas Warrant Mark is copyright material, I have asked
 * for and received the following explicit statement from Habeas:
 *
 * -----------------------------------
 * From: Lindsey Pettit [mailto:support@habeas.com]
 * Sent: Sunday, September 29, 2002 5:51
 * To: Noel J. Bergman
 * Subject: RE: Habeas and Apache James
 *
 * Dear Noel,
 *
 * > FURTHERMORE, if James is to be capable of sending Habeas SWE, I need
 * > to write a Mailet that attaches the headers.  As with any MTA, it
 * > would be up to the administrator to properly configure James and make
 * > sure that licenses are acquired.  Since the Habeas Warrant Mark is
 * > copyright, I believe that I require authorization from you for that
 * > Mailet, especially since it attaches the Habeas Warrant Mark.  For my
 * > own protection, please show me why such authorization is unnecessary,
 * > send me a digitally signed e-mail, or FAX a signed authorization
 *
 * You do not yourself need the authorization to build the functionality
 * into the [mailet];  what one needs authorization, in the form of a
 * license, for, is to use the mark *in headers*, in outgoing email.
 * However, please let me know if you would like something more
 * formal, and I can try to have something faxed to you.
 *
 * > The Mailet docs would reference the Habeas website, and inform
 * > administrators that in order to USE the mailet, they need to ensure
 * > that they have whatever licenses are required from you as appropriate
 * > to your licensing terms.
 *
 * That's absolutely perfect!
 * -----------------------------------
 *
 */

public class AddHabeasWarrantMark extends GenericMailet
{
    /**
     * Called by the mailet container to allow the mailet to process to
     * a message message.
     *
     * This method adds the Habeas Warrant Mark headers to the message,
     * saves the changes, and then allows the message to fall through
     * in the pipeline.
     *
     * @param mail - the Mail object that contains the message and routing information
     * @throws javax.mail.MessagingException - if an message or address parsing exception occurs or
     *      an exception that interferes with the mailet's normal operation
     */
    public void service(Mail mail) throws javax.mail.MessagingException
    {
        try
        {
            javax.mail.internet.MimeMessage message = mail.getMessage();

            for(int i = 0 ; i < HasHabeasWarrantMark.warrantMark.length ; i++)
            {
                message.setHeader(HasHabeasWarrantMark.warrantMark[i][0], HasHabeasWarrantMark.warrantMark[i][1]);
            }

            message.saveChanges();
        }
        catch (javax.mail.MessagingException me)
        {
            log(me.getMessage());
        }
    }

    /*
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo()
    {
        return "Add Habeas Warrant Mark.  Must be used in accordance with a license from Habeas (see http://www.habeas.com for details).";
    }
}
