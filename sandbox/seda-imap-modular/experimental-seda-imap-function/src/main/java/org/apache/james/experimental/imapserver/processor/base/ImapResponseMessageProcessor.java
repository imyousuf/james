package org.apache.james.experimental.imapserver.processor.base;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ImapProcessor;
import org.apache.james.api.imap.ImapSession;
import org.apache.james.api.imap.message.response.ImapResponseMessage;

public class ImapResponseMessageProcessor extends AbstractChainedImapProcessor {

    public ImapResponseMessageProcessor(final ImapProcessor next) {
        super(next);
    }

    protected ImapResponseMessage doProcess(ImapMessage acceptableMessage, ImapSession session) {
        final ImapResponseMessage result = (ImapResponseMessage) acceptableMessage;
        return result;
    }

    protected boolean isAcceptable(ImapMessage message) {
        final boolean result = (message instanceof ImapResponseMessage);
        return result;
    }
}
