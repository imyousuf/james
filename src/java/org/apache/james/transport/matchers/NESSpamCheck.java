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

import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.Mail;
import org.apache.oro.text.regex.MalformedPatternException;

import javax.mail.MessagingException;

/**
 * This is based on a sample filter.cfg for a Netscape Mail Server to stop
 * spam.
 *
 */
public class NESSpamCheck extends GenericRegexMatcher {
    protected Object NESPatterns[][] = {{RFC2822Headers.RECEIVED, "GAA.*-0600.*EST"},
    {RFC2822Headers.RECEIVED, "XAA.*-0700.*EDT"},
    {RFC2822Headers.RECEIVED, "xxxxxxxxxxxxxxxxxxxxx"},
    {RFC2822Headers.RECEIVED, "untrace?able"},
    {RFC2822Headers.RECEIVED, "from (baby|bewellnet|kllklk) "},
    {RFC2822Headers.TO, "Friend@public\\.com"},
    {RFC2822Headers.TO, "user@the[-_]internet"},
    {RFC2822Headers.DATE, "/[0-9]+/.+[AP]M.+Time"},
    {RFC2822Headers.SUBJECT, "^\\(?ADV?[:;)]"},
    {RFC2822Headers.MESSAGE_ID, "<>"},
    {RFC2822Headers.MESSAGE_ID_VARIATION, "<>"},
    {RFC2822Headers.MESSAGE_ID_VARIATION, "<(419\\.43|989\\.28)"},
    {"X-MimeOLE", "MimeOLE V[^0-9]"},
            //Added 20-Jun-1999.  Appears to be broken spamware.
    {"MIME-Version", "1.0From"},
            //Added 28-July-1999.  Check X-Mailer for spamware.
    {"X-Mailer", "DiffondiCool"},
    {"X-Mailer", "Emailer Platinum"},
    {"X-Mailer", "eMerge"},
    {"X-Mailer", "Crescent Internet Tool"},
            //Added 4-Apr-2000.  Check X-Mailer for Cybercreek Avalanche
    {"X-Mailer", "Avalanche"},
            //Added 21-Oct-1999.  Subject contains 20 or more consecutive spaces
    {"Subject", "                    "},
            //Added 31-Mar-2000.  Invalid headers from MyGuestBook.exe CGI spamware
    {"MessageID", "<.+>"},
    {"X-References", "0[A-Z0-9]+, 0[A-Z0-9]+$"},
    {"X-Other-References", "0[A-Z0-9]+$"},
    {"X-See-Also", "0[A-Z0-9]+$"},
            //Updated 28-Apr-1999.  Check for "Sender", "Resent-From", or "Resent-By"
            // before "X-UIDL".  If found, then exit.
    {RFC2822Headers.SENDER, ".+"},
    {RFC2822Headers.RESENT_FROM, ".+"},
    {"Resent-By", ".+"},
            //Updated 19-May-1999.  Check for "X-Mozilla-Status" before "X-UIDL".
    {"X-Mozilla-Status", ".+"},
            //Updated 20-Jul-1999.  Check for "X-Mailer: Internet Mail Service"
            // before "X-UIDL".
    {"X-Mailer", "Internet Mail Service"},
            //Updated 25-Oct-1999.  Check for "X-ID" before "X-UIDL".
    {"X-ID", ".+"},
            //X-UIDL is a POP3 header that should normally not be seen
    {"X-UIDL", ".*"},
            //Some headers are valid only for the Pegasus Mail client.  So first check
            //for Pegasus header and exit if found.  If not found, check for
            //invalid headers: "Comments: Authenticated sender", "X-PMFLAGS" and "X-pmrqc".
    {"X-mailer", "Pegasus"},
            //Added 27-Aug-1999.  Pegasus now uses X-Mailer instead of X-mailer.
    {"X-Mailer", "Pegasus"},
            //Added 25-Oct-1999.  Check for X-Confirm-Reading-To.
    {"X-Confirm-Reading-To", ".+"},
            //Check for invalid Pegasus headers
    {RFC2822Headers.COMMENTS, "Authenticated sender"},
    {"X-PMFLAGS", ".*"},
    {"X-Pmflags", ".*"},
    {"X-pmrqc", ".*"},
    {"Host-From:envonly", ".*"}};

    public void init() throws MessagingException {
        //No condition passed... just compile a bunch of regular expressions
        try {
            compile(NESPatterns);
        } catch(MalformedPatternException mp) {
            throw new MessagingException("Could not initialize NES patterns", mp);
        }
    }
}
