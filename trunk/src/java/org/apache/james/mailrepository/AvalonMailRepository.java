/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import org.apache.avalon.framework.activity.Initializable;
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
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.util.Lock;

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
    implements MailRepository, Component, Configurable, Composable, Initializable {

    private Lock lock;
    protected static boolean DEEP_DEBUG = false;
    private static final String TYPE = "MAIL";
    private Store store;
    private StreamRepository sr;
    private ObjectRepository or;
    private MailStore mailstore;
    private String destination;


    public void configure(Configuration conf) throws ConfigurationException {
        destination = conf.getAttribute("destinationURL");
        getLogger().debug("AvalonMailRepository.destinationURL: " + destination);
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
        store = (Store)componentManager.
            lookup( "org.apache.avalon.cornerstone.services.store.Store" );
    }

    public void initialize()
            throws Exception {
        try {
            //prepare Configurations for object and stream repositories
            DefaultConfiguration objectConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:AvalonFileRepository.compose()" );

            objectConfiguration.setAttribute("destinationURL", destination);
            objectConfiguration.setAttribute("type", "OBJECT");
            objectConfiguration.setAttribute("model", "SYNCHRONOUS");

            DefaultConfiguration streamConfiguration
                = new DefaultConfiguration( "repository",
                                            "generated:AvalonFileRepository.compose()" );

            streamConfiguration.setAttribute( "destinationURL", destination );
            streamConfiguration.setAttribute( "type", "STREAM" );
            streamConfiguration.setAttribute( "model", "SYNCHRONOUS" );

            sr = (StreamRepository) store.select(streamConfiguration);
            or = (ObjectRepository) store.select(objectConfiguration);
            lock = new Lock();
            getLogger().debug(this.getClass().getName() + " created in " + destination);
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error( message, e );
            throw e;
        }
    }

    public synchronized boolean unlock(String key) {
        if (lock.unlock(key)) {
            notifyAll();
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean lock(String key) {
        if (lock.lock(key)) {
            notifyAll();
            return true;
        } else {
            return false;
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
        if(DEEP_DEBUG) {
            getLogger().debug("Retrieving mail: " + key);
        }
        try {
            MailImpl mc = (MailImpl) or.get(key);
            MimeMessageAvalonSource source = new MimeMessageAvalonSource(sr, key);
            mc.setMessage(new MimeMessageWrapper(source));

            return mc;
        } catch (Exception me) {
            if (me instanceof IOException) {
				getLogger().error("IOException retrieving mail: " + me + ", so we're deleting it... good ridance!");
				remove(key);
				return null;
			}
			/*
            if (me instanceof FileNotFoundException ||
            	me instanceof EOFException) {
                remove(key);
                return null;
            }
            */
            getLogger().error("Exception retrieving mail: " + me);
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
    }

    public void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public void remove(String key) {
        if (lock(key)) {
            try {
                sr.remove(key);
                or.remove(key);
            } finally {
                unlock(key);
            }
        } else {
            throw new RuntimeException("Cannot lock " + key + " to remove it");
        }
    }

    public Iterator list() {
        return or.list();
    }
}
