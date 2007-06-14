package org.apache.james.imapserver.processor.base;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.response.imap4rev1.legacy.BadResponse;

public class UnknownRequestImapProcessor extends AbstractLogEnabled implements ImapProcessor {

    public ImapResponseMessage process(ImapMessage message, ImapSession session) {
        Logger logger = getLogger();
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Unknown message: " + message);
        }
        final ImapResponseMessage result;
        if (message instanceof ImapRequest) {
            ImapRequest request = (ImapRequest) message;
            result = new BadResponse("Unknown command.", request.getTag());
        } else {
            result = new BadResponse("Unknown command.");
        }
        return result;
    }

}
