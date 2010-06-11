package org.apache.james.imapserver.mock;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.TestConstants;
import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailRepository;

public class MockMailServer implements MailServer, TestConstants
{

    public void sendMail(MailAddress sender, Collection recipients,
            MimeMessage msg) throws MessagingException
    {
        throw new RuntimeException("not implemented");

    }

    public void sendMail(MailAddress sender, Collection recipients,
            InputStream msg) throws MessagingException
    {
        throw new RuntimeException("not implemented");
    }

    public void sendMail(Mail mail) throws MessagingException
    {
        throw new RuntimeException("not implemented");
    }

    public void sendMail(MimeMessage message) throws MessagingException
    {
        throw new RuntimeException("not implemented");
    }

    Map userToMailRepo = new HashMap();

    public MailRepository getUserInbox(String userName) {
        return null;
    }

    public String getId()
    {
        throw new RuntimeException("not implemented");
    }

    public boolean addUser(String userName, String password)
    {
        throw new RuntimeException("not implemented");
    }

    public boolean isLocalServer(String serverName)
    {
        throw new RuntimeException("not implemented");
    }

}
