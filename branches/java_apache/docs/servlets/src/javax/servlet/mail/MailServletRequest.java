/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */

package javax.servlet.mail;

import javax.servlet.*;
import java.io.*;

/**
 * This interface defines the methods needed by the mail servlet to gather
 * information about the mail message that has been posted and routed
 * to the called mail servlet.
 *
 * <P>Some methods found in ServletRequest are redefined here for more specific
 * behavior in mail handling. The other methods behave like in other servlet
 * paradigms.
 *
 * @author Stefano Mazzocchi <stefano@apache.org>
 * @author Pierpaolo Fumagalli <p.fumagalli@fumagalli.org>
 * @version pre-draft 1.0 (submitted for review)
 */
public interface MailServletRequest extends ServletRequest {
    
    /**
     * Returns the length of the body of the posted mail message
     * without headers and additional information.
     */    
    public abstract int getContentLength();

    /** 
     * Returns the binary input stream associated with the 
     * body of the message (headers are skipped)
     */
    public abstract ServletInputStream getInputStream()
        throws IOException;

    /**
     * Returns the protocol used to send the mail message.
     * (i.e. SMTP, ESMTP, whatever comes next...)
     */
    public abstract String getProtocol();

    /**
     * Returns the reader associated with the body of the message.
     */
    public abstract BufferedReader getReader()
        throws IOException;

    /**
     * Returns the fully qualified name of the last host that handled the mail
     * message (the one performing this request)
     */ 
    public abstract String getRemoteHost();

    /**
     * Returns the IP address of the host returned by getRemoteHost()
     */ 
    public abstract String getRemoteAddr();

    /**
     * Same as getProtocol() (this is due to the fact that there is no
     * standard URL for mail handling: should we define one? what about
     * something like <code>mail://host/user/folder?Subject="Hi"</code>)
     */
    public abstract String getScheme();

    /** 
     * Returns the name of the mail address that received the mail request.
     */
    public abstract MailAddress getServletAddress();

    /**
     * Returns the fully qualified mail address of the sender.
     */
    public abstract MailAddress getSender();
    
    /**
     * Returns an array of fully qualified mail addresses of the
     * recipients of this mail message.
     */
    public abstract MailAddress[] getRecipients();
    
    /**
     * Returns an object encapsulating all the mail headers.
     */
    public abstract MailHeaders getHeaders();
}

    
