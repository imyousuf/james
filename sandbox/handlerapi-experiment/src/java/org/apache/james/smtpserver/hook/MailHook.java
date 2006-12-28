package org.apache.james.smtpserver.hook;

import org.apache.james.smtpserver.SMTPSession;
import org.apache.mailet.MailAddress;

public interface MailHook {

    public HookResult doMail(SMTPSession session, MailAddress sender);
}
