package org.apache.james.smtpserver;
public interface IOObject {
    SMTPResponse nextHandler(SMTPSession session);
}

