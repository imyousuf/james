/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.smtpserver.*;

/**
 * This type was created in VisualAge.
 */
public interface MailServletContext {

    /**
     * This generates a unique message ID for this server (used in creating new messages)
     * @return java.lang.String
     */
    public String getMessageID();

    /**
     * This returns specific server wide properties, as allowed by the server
     * @return java.lang.String
     */
    public String getProperty(String name);

    /**
     * Log a message.
     * @param message java.lang.String
     */
    public void log(String message);

    /**
     * Send a MimeMessage out with the following delivery state
     * @param msg javax.mail.internet.MimeMessage
     * @param state org.apache.james.DeliveryState
     */
    public void sendMessage(MimeMessage msg, DeliveryState state) throws MessagingException;
}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

