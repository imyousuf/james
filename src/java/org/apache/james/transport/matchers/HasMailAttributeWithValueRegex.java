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


import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;
import org.apache.mailet.MatcherConfig;
import java.util.Collection;
import javax.mail.MessagingException;
import java.io.Serializable;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * <P>This Matcher determines if the mail contains the attribute specified in the
 * condition and that attribute matches the supplied regular expression,
 * it returns all recipients if that is the case.</P>
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="HasMailAttributeWithValueRegex=whatever,<regex>" class=&quot;&lt;any-class&gt;&quot;&gt;
 * </CODE></PRE>
 * Note: as it is not possible to put arbitrary objects in the configuration,
 * toString() is called on the attribute value, and that is the value matched against.
 *
 * @version CVS $Revision: 1.1 $ $Date: 2003/09/22 12:08:48 $
 * @since 2.2.0
 **/
public class HasMailAttributeWithValueRegex extends GenericMatcher 
{
    
    private String attributeName;
    private Perl5Matcher matcher  = new Perl5Matcher();
    private Pattern pattern   = null;

    /**
     * Return a string describing this matcher.
     *
     * @return a string describing this matcher
     */
    public String getMatcherInfo() {
        return "Has Mail Attribute Value Matcher";
    }

    public void init (MatcherConfig conf) throws MessagingException
    {
        String condition = conf.getCondition();
        int idx = condition.indexOf(',');
        if (idx != -1) {
            attributeName = condition.substring(0,idx).trim();
            String pattern_string = condition.substring (idx+1, condition.length()).trim();
            try {
                Perl5Compiler compiler = new Perl5Compiler();
                pattern = compiler.compile(pattern_string);
            } catch(MalformedPatternException mpe) {
                throw new MessagingException("Malformed pattern: " + pattern_string, mpe);
            }
        } else {
            throw new MessagingException ("malformed condition for HasMailAttributeWithValueRegex. must be of the form: attr,regex");
        }
    }

    /**
     * @param mail the mail to check.
     * @return all recipients if the part of the condition prior to the first equalsign
     * is the name of an attribute set on the mail and the part of the condition after
     * interpreted as a regular expression matches the toString value of the
     * corresponding attributes value.
     **/
    public Collection match (Mail mail) throws MessagingException
    {
        Serializable obj = mail.getAttribute (attributeName);
        //to be a little more generic the toString of the value is what is matched against
        if ( obj != null && matcher.matches(obj.toString(), pattern)) {
            return mail.getRecipients();
        } 
        return null;
    }
    
}
