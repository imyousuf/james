package org.apache.james.imapserver;

import javax.mail.internet.InternetAddress;

public interface IMAPTest
{
    public int PORT = 143;
    public String HOST = "localhost";

    public String USER = "imapuser";
    public String PASSWORD = "password";
    public String FROM_ADDRESS = "sender@somewhere";
    public String TO_ADDRESS = USER + "@" + HOST;
    
}
