package org.apache.james.experimental.imapserver.processor.base;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ImapProcessor;
import org.apache.james.api.imap.ImapSession;
import org.apache.james.api.imap.message.response.ImapResponseMessage;

abstract public class AbstractChainedImapProcessor extends AbstractLogEnabled
        implements ImapProcessor {

    private final ImapProcessor next;

    /**
     * Constructs a chainable <code>ImapProcessor</code>.
     * 
     * @param next
     *            next <code>ImapProcessor</code> in the chain, not null
     */
    public AbstractChainedImapProcessor(final ImapProcessor next) {
        this.next = next;
    }

    public void enableLogging(Logger logger) {
        super.enableLogging(logger);
        setupLogger(next);
    }

    public ImapResponseMessage process(final ImapMessage message,
            final ImapSession session) {
        final ImapResponseMessage result;
        final boolean isAcceptable = isAcceptable(message);
        if (isAcceptable) {
            result = doProcess(message, session);
        } else {
            result = next.process(message, session);
        }
        return result;
    }

    /**
     * Is the given message acceptable?
     * 
     * @param message
     *            <code>ImapMessage</code>, not null
     * @return true if the given message is processable by this processable
     */
    abstract protected boolean isAcceptable(final ImapMessage message);

    /**
     * Processes an acceptable message. Only messages passing
     * {@link #isAcceptable(ImapMessage)} should be passed to this method.
     * 
     * @param acceptableMessage
     *            <code>ImapMessage</code>, not null
     * @param session
     *            <code>ImapSession</code>, not null
     * @return <code>ImapResponseMessage</code>, not null
     */
    abstract protected ImapResponseMessage doProcess(
            final ImapMessage acceptableMessage, final ImapSession session);
}
