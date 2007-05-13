package org.apache.james.experimental.imapserver.commands;

import org.apache.james.experimental.imapserver.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.CommandTemplate;
import org.apache.james.experimental.imapserver.message.ImapRequestMessage;

public class MockCommand extends CommandTemplate {

    public static final String NAME = "MOCK";

    public String getArgSyntax() {
        return null;
    }

    public String getName() {
        return NAME;
    }

    protected ImapRequestMessage decode(ImapRequestLineReader request, String tag) throws ProtocolException {
        // TODO implementation
        return null;
    }


}
