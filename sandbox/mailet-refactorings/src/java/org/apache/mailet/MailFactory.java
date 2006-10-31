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

package org.apache.mailet;

import java.util.Collection;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


/**
 * @author angusd 
 * @author $Author$ 
 * @version $Revision$
 */
public interface MailFactory {

    /**
     * @param id
     * @param sender
     * @param recipients
     * @param message
     * @return
     * @throws MessagingException 
     */
    Mail newMail(String id, MailAddress sender, Collection recipients, MimeMessage message) throws MessagingException;

    /**
     * @param originalMail
     * @return
     * @throws MessagingException 
     */
    Mail newMail(Mail originalMail) throws MessagingException;
    
    
}


/* 
 *
 * PVCS Log History:
 * $Log$
 *
 */
