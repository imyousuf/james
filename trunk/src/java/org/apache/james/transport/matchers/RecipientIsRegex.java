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

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

import org.apache.oro.text.regex.*;

/**
 * Uses regular expression
 *
 *<p>&lt;mailet match=&quot;RecipientMatch=&lt;regular-expression&gt;&quot;
class=&quot;&lt;any-class&gt;&quot;&gt;</p>
 * <p>The example below will match any recipient in the format
user@log.anything</p>
 * <p>&lt;mailet match=&quot;RecipientMatch=(.*)@log\.(.*)&quot;
class=&quot;&lt;any-class&gt;&quot;&gt;<br>
 * &lt;/mailet&gt;<br>
 * </p>
 * @version 1.0.0, 27/06/2002
 */

public class RecipientIsRegex extends GenericRecipientMatcher {

   // Create Perl5Compiler and Perl5Matcher instances.
    Perl5Compiler compiler = new Perl5Compiler();
    Perl5Matcher matcher  = new Perl5Matcher();
    Pattern pattern   = null;

    public void init() throws javax.mail.MessagingException {
        try {
            pattern = compiler.compile( getCondition() );
        } catch(MalformedPatternException e) {
            e.printStackTrace();
        }
    }

    public boolean matchRecipient(MailAddress recipient) {
        String myRecipient = recipient.toString();
        if ( matcher.matches( myRecipient, pattern)  ){
            return true;
        } else {
            return false;
        }
    }
}
