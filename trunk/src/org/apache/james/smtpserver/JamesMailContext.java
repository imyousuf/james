/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;

/**
 * This type was created in VisualAge.
 */
public class JamesMailContext implements MailServletContext {

    private Context context;
    private Configuration conf;
    private Logger logger;
    private MessageSpool spool;
    private int Count = 0;

    protected Vector validNames = null;

    public final static String SERVER_NAME = "server.name";
    public final static String POSTMASTER = "postmaster";
    public final static String SERVER_TYPE = "server.type";

    /**
     * JamesMailContext constructor comment.
     */
    public JamesMailContext(Context c) throws Exception {

        this.context = c;
        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        this.spool = (MessageSpool) context.getImplementation("spool");

        validNames = new Vector();
        validNames.addElement("server.name");
        validNames.addElement("postmaster");

    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     */
    public String getMessageID() {
        try {
            return ((SMTPServer.MessageId) context.getImplementation("messageid")).getNew();
        } catch (Exception e) {
            Count++;
            return new String("EXCEPTION-IN-GETMESSAGEID-" + new Integer(Count));
        }
    }

    /**
     * If the property is something we allow to be queried, we'll return it to the servlet
     * @return java.lang.String
     * @param name java.lang.String
     */
    public String getProperty(String name) {
        if (validNames.contains(name)) {
            try {
                return conf.getChild(name).getValueAsString();
            } catch (Exception e) {
                return null;
            }
        } 

        return null;
    }

    /**
     * log method comment.
     */
    public void log(String message) {
        logger.log(message);
    }

    /**
     * We add the message and delivery state to the spool for processing
     * @param msg MimeMessage
     * @param state org.apache.james.DeliveryState
     */
    public void sendMessage(MimeMessage msg, DeliveryState state) throws MessagingException {

        //Vector recipients = new Vector();
        //InternetAddress[] addr = state.getRecipients();
        //for ( int i=0; i<addr.length; i++) {
        //    recipients.addElement(addr[i]);
        //}
        //OutputStream body = spool.addMessage(this.getMessageID(), (String)state.get(mail.FROM), recipients );
        //server.getSpool().addMessage(msg, state);
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

