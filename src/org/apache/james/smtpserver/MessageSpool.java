/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.james.*;
import org.apache.java.util.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageSpool implements Configurable {

    private Configuration conf;
    private Logger logger;
    private PersistentStore ps;
    private StreamStore ss;
    private long timeout = 5000;
    private Lock lock;
    
    public MessageSpool(Logger logger) {
        this.logger = logger;
    }

    public void init(Configuration conf)
    throws Exception {

        this.conf = conf;
        try {
            ss = new StreamStore(conf.getChild("repository").getValueAsString());
        } catch (Exception e) {
            logger.log("Exception in Stream Store init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }
        try {
            ps = new PersistentStore(conf.getChild("repository").getValueAsString());
        } catch (Exception e) {
            logger.log("Exception in Persistent Store init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }

        this.timeout = conf.getChild("timeout").getValueAsLong();
        lock = new Lock();
    }

    public synchronized String accept() {

        while (true) {
            logger.log("looking for unprocessed mail", "SMTPServer", logger.DEBUG);
            Enumeration e = ps.list();
            while(e.hasMoreElements()) {
                Object o = e.nextElement();
                if (lock.lock(o)) {
                    return new String(o.toString());
                }
            }
            try {
                wait(timeout);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public synchronized MessageContainer retrive(String key) {

        if (!lock.lock(key)) {
            return (MessageContainer) null;
        }
        MessageContainer mc = (MessageContainer) ps.get(key);
        mc.setBodyInputStream( ss.retrive(key) );
        logger.log("Retriving: " + key, "SMTPServer", logger.DEBUG);
        logger.log("  sender: " + mc.getSender(), "SMTPServer", logger.DEBUG);
        for (Enumeration e = mc.getRecipients().elements(); e.hasMoreElements(); ) {
            logger.log("  recipient: " + e.nextElement(), "SMTPServer", logger.DEBUG);
        }
        return mc;
    }

    public synchronized boolean remove(String key) {

        if (!lock.canI(key)) {
            return false;
        }
        logger.log("removing: " + key, "SMTPServer", logger.DEBUG);
        ps.remove(key);
        ss.remove(key);
        lock.unlock(key);
        return true;
    }

    public synchronized OutputStream store(String key, MessageContainer mc) {
        logger.log("Enter store " + key, "Store", Logger.INFO);

        if (!lock.lock(key)) {
            return (OutputStream) null;
        }
        logger.log("storing: " + key, "SMTPServer", logger.DEBUG);
        logger.log("  sender: " + mc.getSender(), "SMTPServer", logger.DEBUG);
        for (Enumeration e = mc.getRecipients().elements(); e.hasMoreElements(); ) {
            logger.log("  recipient: " + e.nextElement(), "SMTPServer", logger.DEBUG);
        }
        ps.store(key, mc);
        OutputStream out = ss.store(key);
        notifyAll();
        return out;
    }

    public synchronized boolean free(Object key) {

        if (lock.unlock(key)) {
            notifyAll();
            return true;
        }
        return false;
    }

    public MessageContainer addMessage(String key, String sender, Vector recipients) {
        logger.log("Store.addMessage", "Store", Logger.INFO);
        MessageContainer mc = new MessageContainer(sender, recipients);
        mc.setBodyOutputStream( store(key, mc) );
        return mc;
    }
    
    public void destroy() throws Exception {
    }
}


 
