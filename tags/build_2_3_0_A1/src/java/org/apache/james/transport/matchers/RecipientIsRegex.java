/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.MessagingException;

/**
 * <P>Matches recipients whose address matches a regular expression.</P>
 * <P>Is equivalent to the {@link SenderIsRegex} matcher but matching on the recipient.</P>
 * <P>Configuration string: a regular expression.</P>
 * <PRE><CODE>
 * &lt;mailet match=&quot;RecipientIsRegex=&lt;regular-expression&gt;&quot; class=&quot;&lt;any-class&gt;&quot;&gt;
 * </CODE></PRE>
 * <P>The example below will match any recipient in the format user@log.anything</P>
 * <PRE><CODE>
 * &lt;mailet match=&quot;RecipientIsRegex=(.*)@log\.(.*)&quot; class=&quot;&lt;any-class&gt;&quot;&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * @version CVS $Revision$ $Date$
 */

public class RecipientIsRegex extends GenericRecipientMatcher {
    Pattern pattern   = null;

    public void init() throws javax.mail.MessagingException {
        String patternString = getCondition();
        if (patternString == null) {
            throw new MessagingException("Pattern is missing");
        }
        
        patternString = patternString.trim();
        Perl5Compiler compiler = new Perl5Compiler();
        try {
            pattern = compiler.compile(patternString, Perl5Compiler.READ_ONLY_MASK);
        } catch(MalformedPatternException mpe) {
            throw new MessagingException("Malformed pattern: " + patternString, mpe);
        }
    }

    public boolean matchRecipient(MailAddress recipient) {
        String myRecipient = recipient.toString();
        Perl5Matcher matcher  = new Perl5Matcher();
        if (matcher.matches(myRecipient, pattern)){
            return true;
        } else {
            return false;
        }
    }
}
