package org.apache.james.imapserver.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.util.CRLFOutputStream;

public class MessageGenerator
{
    private static Random random;

    protected static synchronized Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;

    }
    
    public static int calculateSize(MimeMessage m) throws IOException, MessagingException {
        ByteArrayOutputStream os =new ByteArrayOutputStream();
        m.writeTo(os);
        return os.size();
    }

    public static MimeMessage generateSimpleMessage() throws MessagingException {
        
        MimeMessage mm = new MimeMessage((Session) null);
        int r = getRandom().nextInt() % 100000;
        int r2 = getRandom().nextInt() % 100000;
        mm.setSubject("good news" + r);
        mm.setFrom(new InternetAddress("user" + r + "@localhost"));
        mm.setSentDate(new Date());
        mm.setRecipients(Message.RecipientType.TO,
                new InternetAddress[] { new InternetAddress("user" + r2
                        + "@localhost") });
        String text = "Hello User" + r2
                + "!\r\n\r\nhave a nice holiday.\r\n\r\ngreetings,\nUser" + r
                + "\r\n";
        mm.setText(text);
        return mm;
    }
    public static String messageContentToString(Message mm) throws IOException, MessagingException {
        ByteArrayOutputStream os=new ByteArrayOutputStream();
        mm.writeTo(new CRLFOutputStream(os));
        return os.toString();
    }

    public static MimeMessage[] generateSimpleMessages(int c) throws MessagingException
    {
        MimeMessage[] msgs=new MimeMessage[c];
        for (int i=0; i<c; i++) {
            msgs[i]=generateSimpleMessage();
        }
        return msgs;
    }
}
