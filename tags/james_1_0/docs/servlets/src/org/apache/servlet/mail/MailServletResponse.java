/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */
 
package org.apache.servlet.mail;

import javax.servlet.ServletResponse;

/**
 * This interface is used by the mail servlet to send the generated
 * mail message.
 * 
 * <P>The mail servlet writer must call the methods defined in this
 * interface before calling the methods of the superclass 
 * ServletResponse.
 *
 * <P>While methods in this interface are used to encapsulate the
 * message body with the proper mail information (such as sender
 * and recipient), the methods inherited from ServletResponse
 * are used to generate and qualify the message body.
 * 
 * <P>If the methods are not called, the default behavior of 
 * each method is to clone exising fields found in the 
 * MailServletRequest. Therefore only modified fields need to 
 * be updated by the mail servlet.
 *
 * @author Stefano Mazzocchi <stefano@apache.org>
 * @author Pierpaolo Fumagalli <p.fumagalli@fumagalli.org>
 * @version pre-draft 1.0 (submitted for review)
 */
public interface MailServletResponse extends ServletResponse {

    /**
     * Sets the fully qualified mail address of the sender.
     * @see org.apache.mail.servlet.MailAddress
     */
    public abstract void setSender(MailAddress sender);
    
    /**
     * Sets a single recipient of the processed mail massage.
     * @see org.apache.mail.servlet.MailAddress
     */
    public abstract void setRecipient(MailAddress recipient);
    
    /**
     * Sets the recipients of the processed mail massage, showing
     * the complete list of recipients in the message.
     * @see org.apache.mail.servlet.MailAddress
     */
    public abstract void setRecipients(MailAddress[] recipients);

    /**
     * Sets the recipients of the processed mail massage, indicating
     * if the list of recipients is shown.
     * Setting showList to false, forces the servlet engine to
     * remove the other recipients: every message shows only the 
     * recipient actually receiving the mail.
     * @see org.apache.mail.servlet.MailAddress
     */
    public abstract void setRecipients(MailAddress[] recipients, boolean showList);
    
    /**
     * Sets the mail headers passing a MailHeaders container.
     * @see org.apache.servlet.mail.MailHeaders
     */
    public abstract void setHeaders(MailHeaders headers);
}