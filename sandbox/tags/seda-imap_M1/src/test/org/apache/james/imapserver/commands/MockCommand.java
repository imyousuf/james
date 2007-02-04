package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;

public class MockCommand extends CommandTemplate {

    public static final String NAME = "MOCK";

    protected String getArgSyntax() {
        return null;
    }

    public String getName() {
        return NAME;
    }

    protected AbstractImapCommandMessage decode(ImapRequestLineReader request) throws ProtocolException {
        // TODO implementation
        return null;
    }


}
