/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import org.apache.james.nntpserver.repository.NNTPGroup;

import java.io.PrintWriter;

/**
 * formatted Group Information. 
 * Group information is displayed differently depending on the 
 * LIST command parameter
 *
 * @author  Harmeet Bedi <harmeet@kodemuse.com>
 */
interface LISTGroup {

    void show(NNTPGroup group);

    class Factory {
        static LISTGroup ACTIVE(final PrintWriter prt) {
            return new LISTGroup() {
                    public void show(NNTPGroup group) {
                        StringBuffer showBuffer =
                            new StringBuffer(128)
                                    .append(group.getName())
                                    .append(" ")
                                    .append(group.getFirstArticleNumber())
                                    .append(" ")
                                    .append(group.getLastArticleNumber())
                                    .append(" ")
                                    .append((group.isPostAllowed() ? "y":"n"));
                        prt.println(showBuffer.toString());
                    }
                };
        }

        static LISTGroup NEWSGROUPS(final PrintWriter prt) {
            return new LISTGroup() {
                    public void show(NNTPGroup group) {
                        StringBuffer showBuffer =
                            new StringBuffer(128)
                                    .append(group.getName())
                                    .append(" ")
                                    .append(group.getDescription());
                        prt.println(showBuffer.toString());
                    }
                };
        }
    } // class Factory
}
