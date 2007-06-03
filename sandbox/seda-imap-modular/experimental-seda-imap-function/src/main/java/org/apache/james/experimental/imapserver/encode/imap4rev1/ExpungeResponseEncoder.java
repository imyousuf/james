package org.apache.james.experimental.imapserver.encode.imap4rev1;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;
import org.apache.james.experimental.imapserver.encode.base.AbstractChainedImapEncoder;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.ExpungeResponse;

public class ExpungeResponseEncoder extends AbstractChainedImapEncoder {

    public ExpungeResponseEncoder(final ImapEncoder next) {
        super(next);
    }

    public boolean isAcceptable(ImapMessage message) {
        return (message instanceof ExpungeResponse);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer) {
        final ExpungeResponse expungeResponse = (ExpungeResponse) acceptableMessage;
        final int messageSequenceNumber = expungeResponse.getMessageSequenceNumber();
        composer.expungeResponse(messageSequenceNumber);
    }
}
