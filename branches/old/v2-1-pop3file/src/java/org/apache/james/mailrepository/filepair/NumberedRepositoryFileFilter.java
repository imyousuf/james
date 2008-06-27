/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository.filepair;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This filters files based on the extension and is tailored to provide
 * backwards compatibility of the numbered repositories that Avalon does.
 *
 */
public class NumberedRepositoryFileFilter implements FilenameFilter {
    private String postfix;
    private String prefix;

    public NumberedRepositoryFileFilter(final String extension) {
        postfix = extension;
        prefix = "." + RepositoryManager.getName();
    }

    public boolean accept(final File file, final String name) {
        //System.out.println("check: " + name);
        //System.out.println("post: " + postfix);
        if (!name.endsWith(postfix)) {
            return false;
        }
        //Look for a couple of digits next
        int pos = name.length() - postfix.length();
        //We have to find at least one digit... if not then this isn't what we want
        if (!Character.isDigit(name.charAt(pos - 1))) {
            return false;
        }
        pos--;
        while (pos >= 1 && Character.isDigit(name.charAt(pos - 1))) {
            //System.out.println("back one");
            pos--;
        }
        //System.out.println("sub: " + name.substring(0, pos));
        //Now want to check that we match the rest
        return name.substring(0, pos).endsWith(prefix);
    }
}


