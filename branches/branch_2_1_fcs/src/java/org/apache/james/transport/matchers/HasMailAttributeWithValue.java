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

import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;
import org.apache.mailet.MatcherConfig;

/**
 * <p>This Matcher determines if the mail contains the attribute specified in
 * the condition and if the value answered when the method toString() is 
 * invoked on the attribute is equal to the String value specified in the
 * condition. If both tests are true, all recipients are returned, else null.
 * </p>
 * 
 * <p>Notes:</p>
 * <p>The current matcher implementation expects a single String value to match
 * on. This matcher requires two values, the attribute name and attribute
 * value. This requires some implicit rules to govern how the single value
 * supplied to the matcher is parsed into two values.</p> 
 * <ul>
 * <li>In the match condition, the split between the attribute name and the
 * attribute value is made at the first comma. Attribute names that include
 * a comma will parse incorrectly and therefore are not supported by this
 * matcher.
 * </li>
 * <li>Leading and trailing spaces are removed from both the attribute name and
 * attribute value specified in the condition and the tested attribute value in
 * the mail prior to matching. Therefore, "abc" , " abc", "abc " and " abc " 
 * are considered equivalent.
 * </li>
 * <li>To test for an empty string, do not specify an attribute value after the
 * comma.
 * </li>
 * </ul>
 * 
 * <p>Sample configuration:</p>
 * <pre><code>
 * &lt;mailet match="HasMailAttributeWithValue=name, value" class=&quot;&lt;any-class&gt;&quot;&gt;
 * </code></pre>
 *
 * @version CVS $Revision: 1.1.2.1 $ $Date: 2003/09/19 14:53:58 $
 * @since 2.2.0
 **/
public class HasMailAttributeWithValue extends GenericMatcher
{

    /**
     * The name of the attribute to match
     */    
    private String fieldAttributeName;

    /**
     * The value of the attribute to match
     */        
    private String fieldAttributeValue;
    

    /**
     * <p>Answers the recipients of the mail if the attribute is present,
     * and has a toString() value equal to the configured value.</p>
     * 
     * @see org.apache.mailet.Matcher#match(Mail)
     */
    public Collection match(Mail mail) throws MessagingException
    {
        Object attributeValue = mail.getAttribute(getAttributeName());

        if (attributeValue != null
            && attributeValue.toString().trim().equals(getAttributeValue()))
            return mail.getRecipients();
        return null;
    }

    /**
     * Returns the attributeName.
     * @return String
     */
    protected String getAttributeName()
    {
        return fieldAttributeName;
    }

    /**
     * Returns the attributeValue.
     * @return String
     */
    protected String getAttributeValue()
    {
        return fieldAttributeValue;
    }

    /**
     * Sets the attributeName.
     * @param attributeName The attributeName to set
     */
    protected void setAttributeName(String attributeName)
    {
        fieldAttributeName = attributeName;
    }

    /**
     * Sets the attributeValue.
     * @param attributeValue The attributeValue to set
     */
    protected void setAttributeValue(String attributeValue)
    {
        fieldAttributeValue = attributeValue;
    }

    /**
     * @see org.apache.mailet.Matcher#init(MatcherConfig)
     */
    public void init(MatcherConfig config) throws MessagingException
    {
        super.init(config);
        String condition = config.getCondition().trim();
        int commaPosition = condition.indexOf(',');

        if (-1 == commaPosition)
            throw new MessagingException("Syntax Error. Missing ','.");

        if (0 == commaPosition)
            throw new MessagingException("Syntax Error. Missing attribute name.");

        setAttributeName(condition.substring(0, commaPosition).trim());
        setAttributeValue(condition.substring(commaPosition + 1).trim());
    }
    
    /**
     * Return a string describing this matcher.
     *
     * @return a string describing this matcher
     */
    public String getMatcherInfo() {
        return "Has Mail Attribute With Value Matcher";
    }
}
