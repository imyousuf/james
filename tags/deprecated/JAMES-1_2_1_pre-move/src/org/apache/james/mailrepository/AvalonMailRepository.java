/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.mailrepository;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.utils.*;
import java.util.*;
import java.io.*;
import org.apache.mailet.*;
import org.apache.james.core.*;
import javax.mail.internet.*;
import javax.mail.MessagingException;

/**
 * Implementation of a MailRepository on a FileSystem.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class AvalonMailRepository implements SpoolRepository {

    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */

    private Store.StreamRepository sr;
    private Store.ObjectRepository or;
    private String path;
    private String name;
    private String destination;
    private String type;
    private String model;
    private Lock lock;

    public AvalonMailRepository() {
    }

    public void setAttributes(String name, String destination, String type, String model) {
        this.name = name;
        this.destination = destination;
        this.model = model;
        this.type = type;
    }

    public void setComponentManager(ComponentManager comp) {

        Store store = (Store) comp.getComponent(Interfaces.STORE);
        this.sr = (Store.StreamRepository) store.getPrivateRepository(destination, Store.STREAM, model);
        this.or = (Store.ObjectRepository) store.getPrivateRepository(destination, Store.OBJECT, model);
        lock = new Lock();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public String getChildDestination(String childName) {
        return destination + childName.replace ('.', File.separatorChar) + File.separator;
    }

    public synchronized void unlock(Object key) {

        if (lock.unlock(key)) {
            notifyAll();
        } else {
            throw new LockException("Your thread do not own the lock of record " + key);
        }
    }

    public synchronized void lock(Object key) {

        if (lock.lock(key)) {
            notifyAll();
        } else {
            throw new LockException("Record " + key + " already locked by another thread");
        }
    }

    public synchronized void store(MailImpl mc) {
        try {
            String key = mc.getName();
            OutputStream out = sr.store(key);
            mc.writeMessageTo(out);
            out.close();
            or.store(key, mc);
            notifyAll();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing Message Container: " + e);
        }
    }

    public MailImpl retrieve(String key) {
        MailImpl mc = (MailImpl) or.get(key);
        try {
            InputStream in = new AvalonMimeMessageInputStream(sr, key);
            mc.setMessage(in);
            in.close();
        } catch (Exception me) {
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
        return mc;
    }

    public void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public void remove(String key) {
        lock(key);
        sr.remove(key);
        or.remove(key);
        unlock(key);
    }

    public Enumeration list() {
        return sr.list();
    }

    public synchronized String accept() {

        while (true) {
            for(Enumeration e = or.list(); e.hasMoreElements(); ) {
                Object o = e.nextElement();
                if (lock.lock(o)) {
                    return o.toString();
                }
            }
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public synchronized String accept(long delay) {
        while (true) {
            long youngest = 0;
            for (Enumeration e = list(); e.hasMoreElements(); ) {
                String s = e.nextElement().toString();
                if (lock.lock(s)) {
                    //We have a lock on this object... let's grab the message
                    //  and see if it's a valid time.
                    MailImpl mail = retrieve(s);
                    if (mail.getState().equals(Mail.ERROR)) {
                        //Test the time...
                        long timeToProcess = delay + mail.getLastUpdated().getTime();
                        if (System.currentTimeMillis() > timeToProcess) {
                            //We're ready to process this again
                            return s;
                        } else {
                            //We're not ready to process this.
                            if (youngest == 0 || youngest > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                youngest = timeToProcess;
                            }
                        }
                    } else {
                        //This mail is good to go... return the key
                        return s;
                    }
                }
            }
            //We did not find any... let's wait for a certain amount of time
            try {
                if (youngest == 0) {
                    wait();
                } else {
                    wait(youngest - System.currentTimeMillis());
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

}
