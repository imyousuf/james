/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collection;

/**
 * This is a generic matcher that uses regular expressions.  If any of
 * the regular expressions match, the matcher is considered to have
 * matched.  This is an abstract class that must be subclassed to feed
 * patterns.  Patterns are provided by calling the compile method.  A
 * subclass will generally call compile() once during init(), but it
 * could subclass match(), and call it as necessary during message
 * processing (e.g., if a file of expressions changed). 
 *
 * @
 */

abstract public class GenericRegexMatcher extends GenericMatcher {
    protected Object[][] patterns;

    public void compile(Object[][] patterns) throws MalformedPatternException {
        // compile a bunch of regular expressions
        this.patterns = patterns;
        for (int i = 0; i < patterns.length; i++) {
            String pattern = (String)patterns[i][1];
            patterns[i][1] = new Perl5Compiler().compile(pattern, Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.SINGLELINE_MASK);
        }
    }

    abstract public void init() throws MessagingException;

    public Collection match(Mail mail) throws MessagingException {
        MimeMessage message = mail.getMessage();
        Perl5Matcher matcher = new Perl5Matcher();

        //Loop through all the patterns
        if (patterns != null) for (int i = 0; i < patterns.length; i++) {
            //Get the header name
            String headerName = (String)patterns[i][0];
            //Get the patterns for that header
            Pattern pattern = (Pattern)patterns[i][1];
            //Get the array of header values that match that
            String headers[] = message.getHeader(headerName);
            //Loop through the header values
            if (headers != null) for (int j = 0; j < headers.length; j++) {
                if (matcher.matches(headers[j], pattern)) {
                    // log("Match: " + headerName + "[" + j + "]: " + headers[j]);
                    return mail.getRecipients();
                }
                //log("       " + headerName + "[" + j + "]: " + headers[j]);
            }
        }
        return null;
    }
}
