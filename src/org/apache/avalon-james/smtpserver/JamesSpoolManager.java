/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.internet.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.java.lang.*;
import org.apache.java.recycle.*;
import org.apache.james.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class JamesSpoolManager implements Stoppable, Contextualizable {

    private Configuration conf;
    private MessageSpool spool;
    private Logger logger;
    private Store store;
    
    /**
     * SpoolManager constructor comment.
     */
    public JamesSpoolManager() {
    }

    public void init(Context context)
    throws Exception {

        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        this.store = (Store) context.getImplementation(Interfaces.STORE);
        this.spool = (MessageSpool) context.getImplementation("spool");
    }


    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        String key;
        while(true) {
            key = spool.accept();
            MessageContainer mc = spool.retrive(key);
            
            // hereafter there should be some "reactor" snending messages to 
            // the right processor, the remote dispatcher or the local dispatcher.
            logger.log("parsing " + key, "SMTPServer", logger.DEBUG);
            logger.log("  sender: " + mc.sender, "SMTPServer", logger.DEBUG);
            for (Enumeration e = mc.recipients.elements(); e.hasMoreElements(); ) {
                logger.log("  recipient: " + e.nextElement(), "SMTPServer", logger.DEBUG);
            }
            Vector mail = new Vector();
            mail.addElement(key);
            mail.addElement(mc.sender);
            mail.addElement(mc.recipients);
            store.store(key, mail);
            logger.log("deleting " + key, "SMTPServer", logger.DEBUG);
            mc.close();
            spool.remove(key);
        }
    }
    
    public void stop() {
    }
    
    public void destroy()
    throws Exception {
    }
}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

