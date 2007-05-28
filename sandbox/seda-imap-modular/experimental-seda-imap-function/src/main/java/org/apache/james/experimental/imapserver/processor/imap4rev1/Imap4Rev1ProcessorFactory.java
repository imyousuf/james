package org.apache.james.experimental.imapserver.processor.imap4rev1;

import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.UsersRepository;

/**
 * TODO: perhaps this should be a POJO
 */
public class Imap4Rev1ProcessorFactory {

    public static final ImapProcessor createDefaultChain(final ImapProcessor chainEndProcessor, 
            final UsersRepository users, final MailboxManagerProvider mailboxManagerProvider) {
        final LogoutProcessor logoutProcessor = new LogoutProcessor(chainEndProcessor);
        final CapabilityProcessor capabilityProcessor = new CapabilityProcessor(logoutProcessor);
        final CheckProcessor checkProcessor = new CheckProcessor(capabilityProcessor);
        final LoginProcessor loginProcessor = new LoginProcessor(checkProcessor, users);
        final RenameProcessor renameProcessor = new RenameProcessor(loginProcessor, mailboxManagerProvider);
        final DeleteProcessor deleteProcessor = new DeleteProcessor(renameProcessor, mailboxManagerProvider);
        final CreateProcessor createProcessor = new CreateProcessor(deleteProcessor, mailboxManagerProvider);
        final CloseProcessor closeProcessor = new CloseProcessor(createProcessor);
        final UnsubscribeProcessor unsubscribeProcessor = new UnsubscribeProcessor(closeProcessor, mailboxManagerProvider);
        final SubscribeProcessor subscribeProcessor = new SubscribeProcessor(unsubscribeProcessor, mailboxManagerProvider);
        final CopyProcessor copyProcessor = new CopyProcessor(subscribeProcessor, mailboxManagerProvider);
        final AuthenticateProcessor authenticateProcessor = new AuthenticateProcessor(copyProcessor);
        final ExpungeProcessor expungeProcessor = new ExpungeProcessor(authenticateProcessor, mailboxManagerProvider);
        final ExamineProcessor examineProcessor = new ExamineProcessor(expungeProcessor, mailboxManagerProvider);
        final AppendProcessor appendProcessor = new AppendProcessor(examineProcessor, mailboxManagerProvider);
        final StoreProcessor storeProcessor = new StoreProcessor(appendProcessor);
        final NoopProcessor noopProcessor = new NoopProcessor(storeProcessor);
        final StatusProcessor statusProcessor = new StatusProcessor(noopProcessor, mailboxManagerProvider);
        final LsubProcessor lsubProcessor = new LsubProcessor(statusProcessor);
        final ListProcessor listProcessor = new ListProcessor(lsubProcessor);
        final SearchProcessor searchProcessor = new SearchProcessor(listProcessor);
        final SelectProcessor selectProcessor = new SelectProcessor(searchProcessor, mailboxManagerProvider);
        final ImapProcessor result = new FetchProcessor(selectProcessor);
        return result;
    }
}
