package org.apache.james.experimental.imapserver.processor.base;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;

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
