package org.apache.james.mailboxmanager.mailbox;

import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public interface BasicMailbox {

    /** @return the key */
    String store(MimeMessage message) throws MessagingException;

    /** @return keys */
    Collection list() throws MessagingException;

    MimeMessage retrieve(String key);

    /**
     * key changes by updating
     *  
     * @param key the current key
     * @return the new key
     */
    String update(String key, MimeMessage message)
            throws MessagingException;

    void remove(String key);
}
