/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * An single client request to an IMAP server, with necessary details for
 * command processing
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 17 Jan 2001
 */
public class ImapRequest {

    private StringTokenizer commandLine;
    private boolean useUIDs;
    private ACLMailbox currentMailbox;
    private String commandRaw;
    private String tag;
    private SingleThreadedConnectionHandler caller;
    private String currentFolder;

    public ImapRequest(SingleThreadedConnectionHandler handler) {
        caller = handler;
    }

    public SingleThreadedConnectionHandler getCaller() {
        return caller;
    }

    public void setCommandLine(StringTokenizer st) {
        commandLine = st;
    }

    public StringTokenizer getCommandLine() {
        return commandLine;
    }

    public void setUseUIDs(boolean state) {
        useUIDs = state;
    }

    public boolean useUIDs() {
        return useUIDs;
    }

    public void setCurrentMailbox(ACLMailbox mbox) {
        currentMailbox = mbox;
    }

    public ACLMailbox getCurrentMailbox() {
        return currentMailbox;
    }

    public void setCommandRaw(String raw) {
        commandRaw = raw;
    }

    public String getCommandRaw() {
        return commandRaw;
    }

    public void setTag(String t) {
        tag = t;
    }

    public String getTag() {
        return tag;
    }

    public void setCurrentFolder(String f) {
        currentFolder = f;
    }

    public String getCurrentFolder() {
        return currentFolder;
    }
}
