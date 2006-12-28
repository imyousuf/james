package org.apache.james.smtpserver.hook;

import org.apache.james.smtpserver.SMTPSession;

public interface HeloHook {

    public HookResult doHelo(SMTPSession session, String helo);
}
