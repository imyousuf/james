/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.*;

/**
 * This type was created in VisualAge.
 */
public class JamesMailContext implements MailServletContext {

    // protected Properties props = null;
    // protected MessageSpool spool = null;

    private JamesServ server = null;
    protected Vector validNames = null;

    /**
     * JamesMailContext constructor comment.
     */
    public JamesMailContext(JamesServ server) {
        this.server = server;
        validNames = new Vector();

        validNames.addElement("server.name");
        validNames.addElement("postmaster");
    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     */
    public String getMessageID() {
        return server.getMessageID();
    }

    /**
     * If the property is something we allow to be queried, we'll return it to the servlet
     * @return java.lang.String
     * @param name java.lang.String
     */
    public String getProperty(String name) {
        if (validNames.contains(name)) {
            return server.getProperty(name);
        } 

        return null;
    }

    /**
     * log method comment.
     */
    public void log(String message) {
        System.out.println(message);
    }

    /**
     * We add the message and delivery state to the spool for processing
     * @param msg MimeMessage
     * @param state org.apache.james.DeliveryState
     */
    public void sendMessage(MimeMessage msg, DeliveryState state) throws MessagingException {
        server.getSpool().addMessage(msg, state);
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

