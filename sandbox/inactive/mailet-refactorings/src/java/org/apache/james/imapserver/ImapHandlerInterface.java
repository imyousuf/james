package org.apache.james.imapserver;

public interface ImapHandlerInterface
{

    void forceConnectionClose(String byeMessage);

    void resetHandler();

}
