/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.mailet.RFC2822Headers;
import javax.mail.MessagingException;

/**
 * Initializes RegexMatcher with regular expressions from a file.
 *
 * @author  Noel J. Bergman <noel@devtech.com>
 */
public class FileRegexMatcher extends GenericRegexMatcher {
    public void init() throws MessagingException {
        try {
            java.io.RandomAccessFile patternSource = new java.io.RandomAccessFile(getCondition(), "r");
            int lines = 0;
            while(patternSource.readLine() != null) lines++;
            patterns = new Object[lines][2];
            patternSource.seek(0);
            for (int i = 0; i < lines; i++) {
                String line = patternSource.readLine();
                patterns[i][0] = line.substring(0, line.indexOf(':'));
                patterns[i][1] = line.substring(line.indexOf(':')+1);
            }
            compile(patterns);
        }
        catch (java.io.FileNotFoundException fnfe) {
            throw new MessagingException("Could not locate patterns.", fnfe);
        }
        catch (java.io.IOException ioe) {
            throw new MessagingException("Could not read patterns.", ioe);
        }
        catch(MalformedPatternException mp) {
            throw new MessagingException("Could not initialize regex patterns", mp);
        }
    }
}
