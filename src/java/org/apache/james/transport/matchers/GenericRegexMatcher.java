/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
 * @author  Serge Knystautas <sergek@lokitech.com>
 * @author  Noel J. Bergman <noel@devtech.com>
 * @
 */

abstract public class GenericRegexMatcher extends GenericMatcher {
    protected Perl5Matcher matcher;
    protected Object[][] patterns;

    public void compile(Object[][] patterns) throws MalformedPatternException {
        // compile a bunch of regular expressions
        this.patterns = patterns;
        matcher = new Perl5Matcher();
        for (int i = 0; i < patterns.length; i++) {
            String pattern = (String)patterns[i][1];
            patterns[i][1] = new Perl5Compiler().compile(pattern);
        }
    }

    abstract public void init() throws MessagingException;

    public Collection match(Mail mail) throws MessagingException {
        MimeMessage message = mail.getMessage();

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
                    return mail.getRecipients();
                }
            }
        }
        return null;
    }
}
