package org.apache.james.experimental.imapserver.encode.imap4rev1;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;
import org.apache.james.experimental.imapserver.encode.base.AbstractChainedImapEncoder;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.FetchResponse;

public class FetchResponseEncoder extends AbstractChainedImapEncoder {

    public FetchResponseEncoder(final ImapEncoder next) {
        super(next);
    }

    public boolean isAcceptable(final ImapMessage message) {
        return (message instanceof FetchResponse);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer) {
        final FetchResponse fetchResponse = (FetchResponse) acceptableMessage;
        // TODO: this is inefficient
        final String data = fetchResponse.getData();
        final int number = fetchResponse.getNumber();
        composer.fetchResponse(number, data);
    }

}
