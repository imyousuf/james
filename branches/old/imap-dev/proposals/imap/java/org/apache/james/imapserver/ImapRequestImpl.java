/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
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

package org.apache.james.imapserver;

import java.util.StringTokenizer;

/**
 * An single client request to an IMAP server, with necessary details for
 * command processing
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
public class ImapRequestImpl implements ImapRequest
{

    private String _command;
    private StringTokenizer commandLine;
    private boolean useUIDs;
    private ACLMailbox currentMailbox;
    private String commandRaw;
    private String tag;
    private SingleThreadedConnectionHandler caller;
    private String currentFolder;

    public ImapRequestImpl(SingleThreadedConnectionHandler handler,
                           String command ) {
        caller = handler;
        _command = command;
    }
    
    public String getCommand()
    {
        return _command;
    }
    
    public void setCommand( String command )
    {
        _command = command;
    }

    public SingleThreadedConnectionHandler getCaller() {
        return caller;
    }

    public void setCommandLine(StringTokenizer st) {
        commandLine = st;
    }

    public StringTokenizer getCommandLine() {
        //return new java.util.StringTokenizer(this.getCommandRaw());
        return commandLine;
    }

    public int arguments()
    {
        return commandLine.countTokens();
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
