/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.*;
import org.apache.james.server.*;
import org.apache.james.util.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageSpool implements Configurable {

    private Properties props;
    private JamesServ serv;
    private LoggerInterface logger;
    private HashMap locks;
    private MultithreadedObjectStore mos;
    private long timeout = 10000;

    public void init(JamesServ serv, Properties props)
    throws Exception {

        this.serv = serv;
        this.props = props;
        this.logger = serv.getLogger();
        this.timeout = Long.parseLong(props.getProperty("timeout"));
        try {
            mos = new MultithreadedObjectStore();
            mos.init(serv, serv.getBranchProperties(props, "multithreadedobjectstore."));
        } catch (Exception e) {
            logger.log("Exception in MultithreadedObjectStore init: " + e.getMessage(), logger.ERROR_LEVEL);
            System.exit(1);
        }
    }

    public synchronized Object accept() {

        while (true) {
            Enumeration e = mos.listUnlocked();
            while(e.hasMoreElements()) {
                Object o = e.nextElement();
                if (mos.lock(o)) {
                    return o;
                }
            }
            try {
                wait(timeout);
            } catch (InterruptedException ignored) {
			}
        }
    }

    public Object retrive(Object key) {

        try {
            return mos.retrive(key);
        } catch (AccessDenyException e) {
            // ????????????
            return null;
        }
    }

    public synchronized void remove(Object key) {

        try {
            mos.remove(key);
        } catch (AccessDenyException e) {
            // ????????????
        }
        notifyAll();
    }

    public synchronized void store(Object key, Object data) {

        try {
            mos.store(key, data);
        } catch (AccessDenyException e) {
            // ????????????
        }
        notifyAll();
    }

    public synchronized void free(Object key) {

        mos.unlock(key);
        notifyAll();
    }

    public void addMessage(MimeMessage message, DeliveryState state) {

        try {
            this.store(message.getMessageID(), new MessageContainer(message, state));
        } catch (MessagingException e) {
            // ????????????
        }
    }
}


 