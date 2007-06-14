package org.apache.james.imapserver.codec.encode.imap4rev1;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.ExpungeResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.AbstractChainedImapEncoder;

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
