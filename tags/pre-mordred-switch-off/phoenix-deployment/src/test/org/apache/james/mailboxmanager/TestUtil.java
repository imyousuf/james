package org.apache.james.mailboxmanager;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.util.CRLFOutputStream;

public class TestUtil {
    
    private static Random random;
    

    public static boolean contentEquals(MimeMessage m1, MimeMessage m2, boolean verbose)
            throws IOException, MessagingException {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        m1.writeTo(new CRLFOutputStream(baos1));
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        m2.writeTo(new CRLFOutputStream(baos2));
        if (verbose) {
            byte[] b1=baos1.toByteArray();
            byte[] b2=baos2.toByteArray();
            int size=b1.length;
            if (b2.length<b1.length) size=b2.length;
            for (int i=0; i< size; i++) {
                if (b1[i]!=b2[i]) {
                    System.out.println("I: "+i+" B1: "+b1[i]+" B2 "+b2[i]);
                    System.out.println("B1:"+new String(b1,0,i+1)+"\u00C2\u00B0");
                    System.out.println("B2:"+new String(b2,0,i+1)+"\u00C2\u00B0");
                    break;
                }
            }
            
        }
        
        return Arrays.equals(baos1.toByteArray(), baos2.toByteArray());
    }

    public static byte[] messageToByteArray(MimeMessage mm)
            throws IOException, MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mm.writeTo(new CRLFOutputStream(baos));
        return baos.toByteArray();
    }

    public static boolean messageSetsEqual(Collection ma1, Collection ma2)
            throws IOException, MessagingException {
        if (ma1.size() != ma2.size())
            return false;
        Set s1 = new HashSet();
        Set s2 = new HashSet();
        for (Iterator it = ma1.iterator(); it.hasNext();) {
            MimeMessage mm = (MimeMessage) it.next();
            HashCodeBuilder builder = new HashCodeBuilder();
            builder.append(messageToByteArray(mm));
            s1.add(new Integer(builder.toHashCode()));
        }
        for (Iterator it = ma2.iterator(); it.hasNext();) {
            MimeMessage mm = (MimeMessage) it.next();
            HashCodeBuilder builder = new HashCodeBuilder();
            builder.append(messageToByteArray(mm));
            s2.add(new Integer(builder.toHashCode()));
        }
        return s1.equals(s2);
    }
    
    
    public static MimeMessage createMessage() throws MessagingException {
        MimeMessage mm = new MimeMessage((Session) null);
        int r = getRandom().nextInt() % 100000;
        int r2 = getRandom().nextInt() % 100000;
        mm.setSubject("good news" + r);
        mm.setFrom(new InternetAddress("user" + r + "@localhost"));
        mm.setRecipients(Message.RecipientType.TO,
                new InternetAddress[] { new InternetAddress("user" + r2
                        + "@localhost") });
        String text = "Hello User" + r2
                + "!\n\nhave a nice holiday.\r\n\r\ngreetings,\nUser" + r
                + "\n";
        mm.setText(text);
        mm.saveChanges();
        return mm;
    }

    public static synchronized Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;

    }


}
