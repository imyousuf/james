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
import org.apache.mailet.MailAddress;

import java.lang.NumberFormatException;

import java.util.Collection;
import java.util.StringTokenizer;

import javax.mail.*;
import javax.mail.internet.*;

/**
 * <P>Matches mails containing a header with a numeric value whose comparison with the specified value is true.
 * If the header is missing in the message, there will be <I>no match</I></P>
 * <P>Configuration string: The headerName, a comparison operator and the numeric headerValue
 * to compare with, <I>space or tab delimited</I>.</P>
 * <P>The comparison operators are: <CODE>&lt, &lt=, ==, &gt=, &gt</CODE>;
 * another set of operators is: <CODE>LT, LE, EQ, GE, GT</CODE>.
 * Also the following operators are accepted: <CODE>=&lt, =, =&gt</CODE>.</P>
 * <P>Example:</P>
 * <PRE><CODE>
 *    &lt;mailet match="CompareNumericHeaderValue=X-MessageIsSpamProbability > 0.9" class="ToProcessor"&gt;
 *       &lt;processor&gt; spam &lt;/processor&gt;
 *    &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * @version CVS $Revision: 1.3 $ $Date: 2003/07/12 06:45:18 $
 * @since 2.2.0
 */
public class CompareNumericHeaderValue extends GenericMatcher {

    private String headerName = null;
    
    private int comparisonOperator;
    private final static int LT = -2;
    private final static int LE = -1;
    private final static int EQ =  0;
    private final static int GE = +1;
    private final static int GT = +2;
    
    private Double headerValue;

    public void init() throws MessagingException {
        StringTokenizer st = new StringTokenizer(getCondition(), " \t", false);
        if (st.hasMoreTokens()) {
            headerName = st.nextToken().trim();
        }
        else {
            throw new MessagingException("Missing headerName");
        }
        if (st.hasMoreTokens()) {
            String comparisonOperatorString = st.nextToken().trim();
            if (comparisonOperatorString.equals("<")
                || comparisonOperatorString.equals("LT")) {
                comparisonOperator = LT;
            }
            else if (comparisonOperatorString.equals("<=")
                     || comparisonOperatorString.equals("=<")
                     || comparisonOperatorString.equals("LE")) {
                comparisonOperator = LE;
            }
            else if (comparisonOperatorString.equals("==")
                     || comparisonOperatorString.equals("=")
                     || comparisonOperatorString.equals("EQ")) {
                comparisonOperator = EQ;
            }
            else if (comparisonOperatorString.equals(">=")
                     || comparisonOperatorString.equals("=>")
                     || comparisonOperatorString.equals("GE")) {
                comparisonOperator = GE;
            }
            else if (comparisonOperatorString.equals(">")
                     || comparisonOperatorString.equals("GT")) {
                comparisonOperator = GT;
            }
            else {
                throw new MessagingException("Bad comparisonOperator: \"" + comparisonOperatorString + "\"");
            }
        }
        else {
            throw new MessagingException("Missing comparisonOperator");
        }
        if (st.hasMoreTokens()) {
            String headerValueString = st.nextToken().trim();
            try {
                headerValue = Double.valueOf(headerValueString);
            }
            catch (NumberFormatException nfe) {
                throw new MessagingException("Bad header comparison value: \""
                                             + headerValueString + "\"", nfe);
            }
        }
        else {
            throw new MessagingException("Missing headerValue threshold");
        }
    }

    public Collection match(Mail mail) throws MessagingException {
        if (headerName == null) {
            // should never get here
            throw new IllegalStateException("Null headerName");
        }
        
        MimeMessage message = (MimeMessage) mail.getMessage();
        
        String [] headerArray = message.getHeader(headerName);
        if (headerArray != null && headerArray.length > 0) {
            try {
                int comparison = Double.valueOf(headerArray[0].trim()).compareTo(headerValue);
                switch (comparisonOperator) {
                    case LT:
                        if (comparison < 0) {
                            return mail.getRecipients();
                        }
                        break;
                    case LE:
                        if (comparison <= 0) {
                            return mail.getRecipients();
                        }
                        break;
                    case EQ:
                        if (comparison == 0) {
                            return mail.getRecipients();
                        }
                        break;
                    case GE:
                        if (comparison >= 0) {
                            return mail.getRecipients();
                        }
                        break;
                    case GT:
                        if (comparison > 0) {
                            return mail.getRecipients();
                        }
                        break;
                    default:
                        // should never get here
                        throw new IllegalStateException("Unknown comparisonOperator" + comparisonOperator);
                }
            }
            catch (NumberFormatException nfe) {
                throw new MessagingException("Bad header value found in message: \"" + headerArray[0] + "\"", nfe);
            }
        }
        
        return null;
    }
}
