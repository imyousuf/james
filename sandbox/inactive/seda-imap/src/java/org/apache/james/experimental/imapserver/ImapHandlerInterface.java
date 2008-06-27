package org.apache.james.experimental.imapserver;

public interface ImapHandlerInterface
{

    void forceConnectionClose(String byeMessage);

    void resetHandler();

}
