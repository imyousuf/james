/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.util;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.*;
import org.apache.james.server.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MultithreadedObjectStore {

    private Properties props;

    private JamesServ serv;

    private LoggerInterface logger;

    private Hashtable locks;

    private ObjectStore os;

    public MultithreadedObjectStore() {
    }

    public void init(JamesServ serv, Properties props)
    throws Exception {

        this.serv = serv;
        this.logger = serv.getLogger();
        try {
            os = (ObjectStore) Class.forName(props.getProperty("ObjectStoreClassName")).newInstance();
            os.init(serv, serv.getBranchProperties(props, "objectstore."));
        } catch (Exception e) {
            logger.log("Exception in ObjectStore init: " + e.getMessage(), logger.ERROR_LEVEL);
            e.printStackTrace();
            System.exit(1);
        }
        locks = new Hashtable();
    }

    public void store(Object key, Object data)
    throws AccessDenyException {

        Object lock = locks.get(key);
        if (lock == null || getCallerId() == lock) {
            os.store(key, data);
        } else {
            throw new AccessDenyException();
        }
    }

    public Object retrive(Object key)
    throws AccessDenyException {

        // Should we lock read access?
        Object lock = locks.get(key);
        if (lock == null || getCallerId() == lock) {
            return os.get(key);
        } else {
            throw new AccessDenyException();

        }
    }

    public void remove(Object key)
    throws AccessDenyException {

        Object lock = locks.get(key);
        if (lock == null || getCallerId() == lock) {
            os.remove(key);
        } else {
            throw new AccessDenyException();
        }
    }

    public Enumeration listAll() {

        return os.list();
    }

    public Enumeration listUnlocked() {

        Vector v = new Vector();
        for (Enumeration e = os.list(); e.hasMoreElements();) {
            Object o = e.nextElement();
            if (locks.get(o) == null) {
                v.addElement(o);
            }
        }
        return v.elements();
    }

    public synchronized boolean lock(Object key) {

        Object lock = locks.get(key);
        if (lock == null) {
            locks.put(key, getCallerId());
            return true;
        } else if(getCallerId() == lock) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean unlock(Object key) {

        Object lock = locks.get(key);
        if (lock == null) {
            return true;
        } else if (getCallerId() == lock) {
            locks.remove(key);
            return true;
        } else {
            return false;
        }
    }

    private Object getCallerId() {
        return Thread.currentThread();
    }
}


 