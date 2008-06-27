/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james;

import org.apache.avalon.blocks.*;
import org.apache.arch.*;
import org.apache.java.util.*;
import java.util.*;
import java.io.*;
import org.apache.mail.Mail;
import javax.mail.internet.*;
import javax.mail.MessagingException;

/**
 * Implementation of a Repository to store Mails.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class MailRepository implements Store.Repository {

    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */
    public final static String MAIL = "MAIL";

    private Store.StreamRepository sr;
    private Store.ObjectRepository or;
    private String path;
    private String name;
    private String destination;
    private String type;
    private String model;
    private Lock lock;

    public MailRepository() {
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

    public synchronized void store(Mail mc) {
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

    public synchronized Mail retrieve(String key) {
        Mail mc = (Mail) or.get(key);
        try {
            mc.setMessage(sr.retrieve(key));
        } catch (Exception me) {
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
        return mc;
    }
    
    public synchronized void remove(Mail mail) {
        remove(mail.getName());
    }

    public synchronized void remove(String key) {
        lock(key);
        sr.remove(key);
        or.remove(key);
        unlock(key);
    }

    public Enumeration list() {
        return sr.list();
    }
}

    
