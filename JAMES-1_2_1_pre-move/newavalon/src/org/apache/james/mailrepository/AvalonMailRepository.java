/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.mailrepository;

import java.io.*;
import java.util.*;
import javax.mail.internet.*;
import javax.mail.MessagingException;

import org.apache.avalon.*;
import org.apache.avalon.services.*;
import org.apache.avalon.util.Lock;
import org.apache.avalon.util.LockException;
import org.apache.log.LogKit;
import org.apache.log.Logger;

import org.apache.mailet.*;
import org.apache.james.core.*;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;


/**
 * Implementation of a MailRepository on a FileSystem.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  <repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/>
 * Requires a logger called MailRepository.
 * 
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public class AvalonMailRepository implements MailRepository, Component, Configurable, Composer {
    protected Lock lock;

    private static final String TYPE = "MAIL";
    private final static boolean        LOG        = true;
    private final static boolean        DEBUG      = LOG && false;
    private Logger logger =  LogKit.getLoggerFor("james.MailRepository");
    private Store store;
    private Store.StreamRepository sr;
    private Store.ObjectRepository or;
    private MailStore mailstore;
    private String destination;

   

    public AvalonMailRepository() {
    }

    public void configure(Configuration conf) throws ConfigurationException {
	destination = conf.getAttribute("destinationURL");
	String checkType = conf.getAttribute("type");
	if (! (checkType.equals("MAIL") || checkType.equals("SPOOL")) ) {
	    logger.warn("Attempt to configure AvalonMailRepository as "
			+ checkType);
	    throw new ConfigurationException("Attempt to configure AvalonMailRepository as " + checkType);
	}
	// ignore model
    }

    public void compose(ComponentManager compMgr) {
	try {
	    store = (Store) compMgr.lookup("org.apache.avalon.services.Store");
	    //prepare Configurations for object and stream repositories
	    DefaultConfiguration objConf
		= new DefaultConfiguration("repository", "generated:AvalonFileRepository.compose()");
	    objConf.addAttribute("destinationURL", destination);
	    objConf.addAttribute("type", "OBJECT");
	    objConf.addAttribute("model", "SYNCHRONOUS");
	    DefaultConfiguration strConf
		= new DefaultConfiguration("repository", "generated:AvalonFileRepository.compose()");
	    strConf.addAttribute("destinationURL", destination);
	    strConf.addAttribute("type", "STREAM");
	    strConf.addAttribute("model", "SYNCHRONOUS");
	    
	    sr = (Store.StreamRepository) store.select(strConf);
	    or = (Store.ObjectRepository) store.select(objConf);
	    lock = new Lock();
	} catch (ComponentNotFoundException cnfe) {
	    if (LOG) logger.error("Failed to retrieve Store component:" + cnfe.getMessage());
	} catch (ComponentNotAccessibleException cnae) {
	    if (LOG) logger.error("Failed to retrieve Store component:" + cnae.getMessage());
	} catch (Exception e) {
	    if (LOG) logger.error("Failed to retrieve Store component:" + e.getMessage());
	}
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
            OutputStream out = sr.put(key);
            mc.writeMessageTo(out);
            out.close();
            or.put(key, mc);
            notifyAll();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing Message Container: " + e);
        }
    }

    public MailImpl retrieve(String key) {
        MailImpl mc = (MailImpl) or.get(key);
        try {
	    InputStream in = new FileMimeMessageInputStream(sr, key);
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

    public Iterator list() {
        return sr.list();
    }


}
