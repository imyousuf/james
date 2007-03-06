package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.message.ImapCommandMessage;

public class MockCommand extends CommandTemplate {

    public static final String NAME = "MOCK";

    public String getArgSyntax() {
        return null;
    }

    public String getName() {
        return NAME;
    }

    protected ImapCommandMessage decode(ImapRequestLineReader request, String tag) throws ProtocolException {
        // TODO implementation
        return null;
    }


}
