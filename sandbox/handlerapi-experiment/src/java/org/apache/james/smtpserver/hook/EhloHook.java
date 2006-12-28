package org.apache.james.smtpserver.hook;

import org.apache.james.smtpserver.SMTPSession;

public interface EhloHook {

    public HookResult doEhlo(SMTPSession session, String ehlo);
}
