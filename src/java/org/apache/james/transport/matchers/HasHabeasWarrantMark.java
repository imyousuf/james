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

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collection;

/*
 * This matcher tests for the Hebeas Warrant Mark.
 * For details see: http://www.hebeas.com
 *
 * Usage: Place this matcher
 *
 * &lt;mailet match="HasHabeasWarrantMark" class="ToProcessor"&gt;
 *     &lt;processor&gt; transport &lt;/processor&gt;
 * &lt;/mailet&gt;
 *
 * in the root processs before the DNSRBL block lists (the InSpammerBlacklist matcher).
 *
 * Because the Habeas Warrant Mark is copyright material, I have asked for and
 * received the following explicit statement from Habeas:
 *
 * -----------------------------------
 * From: Lindsey Pettit [mailto:support@habeas.com]
 * Sent: Sunday, September 29, 2002 5:51
 * To: Noel J. Bergman
 * Subject: RE: Habeas and Apache James
 *
 * Dear Noel,
 *
 * > I guess that since your Warrant Mark is copyright, I should ask for
 * > something from you to explicitly authorize that Hebeas will permit
 * > this code to be included and distributed as part of Apache James
 * > under the Apache Software License.  As we have established, the use
 * > of the Habeas Warrant Mark for filtering is not restricted, but I
 * > would like something to confirm that, so that Apache will be happy.
 *
 * I can hereby confirm to you that there is no license necessary in
 * order to use the Habeas mark for filtering.  That said, however, we
 * do insist that it not ever be used as a basis for rejecting email which
 * bears the Habeas mark.
 * -----------------------------------
 *
 */

public class HasHabeasWarrantMark extends GenericMatcher
{
    public static final String[][] warrantMark =
    {
        { "X-Habeas-SWE-1", "winter into spring" },
        { "X-Habeas-SWE-2", "brightly anticipated" },
        { "X-Habeas-SWE-3", "like Habeas SWE (tm)" },
        { "X-Habeas-SWE-4", "Copyright 2002 Habeas (tm)" },
        { "X-Habeas-SWE-5", "Sender Warranted Email (SWE) (tm). The sender of this" },
        { "X-Habeas-SWE-6", "email in exchange for a license for this Habeas" },
        { "X-Habeas-SWE-7", "warrant mark warrants that this is a Habeas Compliant" },
        { "X-Habeas-SWE-8", "Message (HCM) and not spam. Please report use of this" },
        { "X-Habeas-SWE-9", "mark in spam to <http://www.habeas.com/report/>." },
    };

    public Collection match(Mail mail) throws MessagingException
    {
        MimeMessage message = mail.getMessage();

        //Loop through all the patterns
        for (int i = 0; i < warrantMark.length; i++) try
        {
            String headerName = warrantMark[i][0];                      //Get the header name
            String requiredValue = warrantMark[i][1];                   //Get the required value
            String headerValue = message.getHeader(headerName, null);   //Get the header value(s)

            // We want an exact match, so only test the first value.
            // If there are multiple values, the header may be
            // (illegally) forged.  I'll leave it as an exercise to
            // others if they want to detect and report potentially
            // forged headers.

            if (!(requiredValue.equals(headerValue))) return null;
        }
        catch (Exception e)
        {
            log(e.toString());
            return null;            //if we get an exception, don't validate the mark
        }

        // If we get here, all headers are present and match.
        return mail.getRecipients();
    }

    /*
     * Returns information about the matcher, such as author, version, and copyright.
     * <p>
     * The string that this method returns should be plain text and not markup
     * of any kind (such as HTML, XML, etc.).
     *
     * @return a String containing matcher information
     */

    public String getMatcherInfo()
    {
        return "Habeas Warrant Mark Matcher (see http://www.habeas.com for details).";
    }
}

