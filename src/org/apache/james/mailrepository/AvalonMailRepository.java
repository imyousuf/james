/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.cornerstone.services.store.ObjectRepository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.util.Lock;
import org.apache.james.util.LockException;

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
public class AvalonMailRepository
    extends AbstractLoggable
    implements MailRepository, Component, Configurable, Composable {

    protected Lock lock;
    protected static boolean DEEP_DEBUG = true;
    private static final String TYPE = "MAIL";
    private Store store;
    private StreamRepository sr;
    private ObjectRepository or;
    private MailStore mailstore;
    private String destination;


    public void configure(Configuration conf) throws ConfigurationException {
        destination = conf.getAttribute("destinationURL");
        String checkType = conf.getAttribute("type");
        if (! (checkType.equals("MAIL") || checkType.equals("SPOOL")) ) {
            getLogger().warn( "Attempt to configure AvalonMailRepository as " +
                              checkType);
            throw new ConfigurationException("Attempt to configure AvalonMailRepository as " + checkType);
        }
        // ignore model
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        try {
            store = (Store)componentManager.
                lookup( "org.apache.avalon.cornerstone.services.store.Store" );

            //prepare Configurations for object and stream repositories
            DefaultConfiguration objectConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:AvalonFileRepository.compose()" );

            objectConfiguration.addAttribute("destinationURL", destination);
            objectConfiguration.addAttribute("type", "OBJECT");
            objectConfiguration.addAttribute("model", "SYNCHRONOUS");

            DefaultConfiguration streamConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:AvalonFileRepository.compose()" );

            streamConfiguration.addAttribute( "destinationURL", destination );
            streamConfiguration.addAttribute( "type", "STREAM" );
            streamConfiguration.addAttribute( "model", "SYNCHRONOUS" );

            sr = (StreamRepository) store.select(streamConfiguration);
            or = (ObjectRepository) store.select(objectConfiguration);
            lock = new Lock();
	    getLogger().debug(this.getClass().getName() + " created in " + destination);
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error( message, e );
            throw new ComponentException( message, e );
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
	    if(DEEP_DEBUG) getLogger().debug("Mail " + key + " stored." );
            notifyAll();
        } catch (Exception e) {
	    getLogger().error("Exception storing mail: " + e);
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing Message Container: " + e);
        }
    }

    public MailImpl retrieve(String key) {
	if(DEEP_DEBUG) getLogger().debug("Retrieving mail: " + key);
        MailImpl mc = (MailImpl) or.get(key);
        try {
            InputStream in = new FileMimeMessageInputStream(sr, key);
            mc.setMessage(in);
            in.close();
        } catch (Exception me) {
	    getLogger().error("Exception retrieving mail: " + me);
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
        return mc;
    }

    public void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public void remove(String key) {
        try {
            lock( key);
            sr.remove(key);
            or.remove(key);
        } finally {
            unlock(key);
        }
    }

    public Iterator list() {
        return sr.list();
    }
}
