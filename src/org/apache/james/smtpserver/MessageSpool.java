/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.blocks.Interfaces;
import org.apache.avalon.blocks.Store;
import org.apache.avalon.blocks.Logger;
import org.apache.java.util.*;
import org.apache.james.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageSpool implements Contextualizable {

    private Configuration conf;
    private Context context;
    private org.apache.avalon.blocks.Logger logger;
    private org.apache.avalon.blocks.Store.ObjectRepository or;
    private org.apache.avalon.blocks.Store.StreamRepository sr;
    private org.apache.avalon.blocks.Store store;
    private long timeout = 5000;
    private Lock lock;
    
    public MessageSpool() {
    }

    public void init(Context context)
    throws Exception {

        this.context = context;
        this.conf = context.getConfiguration();
	    this.store = (Store) context.getImplementation(Interfaces.STORE);
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);;
        String path = conf.getChild("repository").getValueAsString();
        try {
            sr = (Store.StreamRepository) store.getPrivateRepository(Store.STREAM, Store.ASYNCHRONOUS);
            sr.setDestination(path);
        } catch (Exception e) {
            logger.log("Exception in Stream Store init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }
        try {
            or = (Store.ObjectRepository) store.getPrivateRepository(Store.OBJECT, Store.ASYNCHRONOUS);
            or.setDestination(path);
        } catch (Exception e) {
            logger.log("Exception in Persistent Store init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }
        logger.log("Mail Spool opened in " + path, "SMTPServer", logger.INFO);

        this.timeout = conf.getChild("timeout").getValueAsLong();
        lock = new Lock();
    }

    public synchronized String accept() {

        while (true) {
            logger.log("looking for unprocessed mail", "SMTPServer", logger.DEBUG);
            Enumeration e = or.list();
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
        MessageContainer mc = (MessageContainer) or.get(key);
        mc.setBodyInputStream( sr.retrive(key) );
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
        or.remove(key);
        sr.remove(key);
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
        or.store(key, mc);
        OutputStream out = sr.store(key);
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


 
