/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.smtpserver;

import java.io.*;
import java.util.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.james.*;
import org.apache.arch.*;

/**
 * SMTPHandlers store incoming message here. When a message is closed (by the 
 * SMTPHandler) it becomes avaiable for JamesSpoolMessage to be parsed.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageSpool implements Component, Composer, Configurable, Service {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private Store store;
    private Store.ObjectRepository or;
    private Store.StreamRepository sr;
    private Lock lock;
    
    public MessageSpool() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

	public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("Mail Spool init....", "SMTPServer", logger.INFO);
        store = (Store) comp.getComponent(Interfaces.STORE);
        String privateRepository = conf.getConfiguration("spoolRepository", ".").getValue();
        try {
            sr = (Store.StreamRepository) store.getPrivateRepository(privateRepository, Store.STREAM, Store.ASYNCHRONOUS);
        } catch (Exception e) {
            logger.log("Exception in Stream Store init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }
        try {
            or = (Store.ObjectRepository) store.getPrivateRepository(privateRepository, Store.OBJECT, Store.ASYNCHRONOUS);
        } catch (Exception e) {
            logger.log("Exception in Persistent Store init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }
        logger.log("Mail Spool opened", "SMTPServer", logger.INFO);
        lock = new Lock();
    }

    public synchronized String accept() {

        while (true) {
            logger.log("looking for unprocessed mail", "SMTPServer", logger.DEBUG);
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

    public synchronized MessageContainer retrieve(String key) {

        if (!lock.lock(key)) {
            throw new LockException("Record " + key + " locked");
        }
        MessageContainer mc = (MessageContainer) or.get(key);
        mc.setBodyInputStream(sr.retrieve(key));
        return mc;
    }

    public synchronized void remove(String key) {

        if (!lock.canI(key)) {
            throw new LockException("Record " + key + " locked");
        }
        or.remove(key);
        sr.remove(key);
        lock.unlock(key);
    }

    public synchronized OutputStream store(String key, MessageContainer mc) {

        if (!lock.lock(key)) {
            throw new LockException("Record " + key + " locked");
        }
        or.store(key, mc);
        return sr.store(key);
    }

    public synchronized void free(Object key) {

        if (lock.unlock(key)) {
            notifyAll();
        } else {
            throw new LockException("Record " + key + " locked");
        }
    }

    public OutputStream addMessage(String key, String sender, Vector recipients) {
        
        MessageContainer mc = new MessageContainer(sender, recipients);
        mc.setMessageId(key);
        return store(key, mc);
    }
    
    public void destroy() throws Exception {
    }
}


 
