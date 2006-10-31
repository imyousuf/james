/*
 * Created on Oct 31, 2006
 *
 * PVCS Workfile Details:
 * $Workfile$
 * $Revision$
 * $Author$
 * $Date$
 * $Modtime$
 */

package org.apache.james;

import java.util.Collection;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailFactory;


/**
 * @author angusd 
 * @author $Author$ 
 * @version $Revision$
 */
public class JamesMailFactory implements MailFactory {

    /**
     * @throws MessagingException 
     * @see org.apache.mailet.MailFactory#newMail(java.lang.String, org.apache.mailet.MailAddress, java.util.Collection, javax.mail.internet.MimeMessage)
     */
    public Mail newMail(String id, MailAddress sender, Collection recipients, MimeMessage message) throws MessagingException {

       
        return new MailImpl(id, sender, recipients, message);
    }

    /**
     * @throws MessagingException 
     * @see org.apache.mailet.MailFactory#newMail(org.apache.mailet.Mail)
     */
    public Mail newMail(Mail originalMail) throws MessagingException {

        
        return  new MailImpl(originalMail);
    }
    
}


/* 
 *
 * PVCS Log History:
 * $Log$
 *
 */
